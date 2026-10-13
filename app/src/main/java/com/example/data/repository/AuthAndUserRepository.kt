package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.AuditLog
import com.example.data.model.PermissionKeys
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AuthAndUserRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = FirebaseAuth.getInstance()

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
    }

    suspend fun getCurrentUserProfile(): UserProfile? {
        val uid = auth.currentUser?.uid ?: return null
        val doc = db.collection("users").document(uid).get().await()
        return if (doc.exists()) doc.toObject(UserProfile::class.java) else null
    }

    fun observeCurrentUserProfile(): Flow<UserProfile?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val reg = db.collection("users").document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, "users/$uid")
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(UserProfile::class.java))
        }
        awaitClose { reg.remove() }
    }

    suspend fun syncOrCreateUserProfile(firebaseUser: com.google.firebase.auth.FirebaseUser): UserProfile {
        val uid = firebaseUser.uid
        val docRef = db.collection("users").document(uid)
        val snapshot = docRef.get().await()

        val isPrimaryOwner = firebaseUser.email?.equals("hk3156140@gmail.com", ignoreCase = true) == true

        if (!snapshot.exists()) {
            val role = if (isPrimaryOwner) UserRole.OWNER.name else UserRole.CUSTOMER.name
            val permissions = if (isPrimaryOwner) PermissionKeys.fullPermissions() else emptyMap()

            val newProfile = UserProfile(
                id = uid,
                userId = uid,
                name = firebaseUser.displayName ?: if (isPrimaryOwner) "ILAM Owner" else "Customer",
                email = firebaseUser.email ?: "",
                phone = firebaseUser.phoneNumber ?: "",
                role = role,
                status = "ACTIVE",
                permissions = permissions
            )
            val data = mapOf(
                "userId" to uid,
                "name" to newProfile.name,
                "email" to newProfile.email,
                "phone" to newProfile.phone,
                "role" to newProfile.role,
                "status" to "ACTIVE",
                "permissions" to newProfile.permissions,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(data).await()
            logAudit("LOGIN_REGISTER", "New user registered: ${newProfile.name} (${newProfile.role})", uid, "USER")
            return newProfile
        } else {
            val existing = snapshot.toObject(UserProfile::class.java)!!
            if (isPrimaryOwner && existing.role != UserRole.OWNER.name) {
                docRef.update(
                    mapOf(
                        "role" to UserRole.OWNER.name,
                        "permissions" to PermissionKeys.fullPermissions(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                return existing.copy(role = UserRole.OWNER.name, permissions = PermissionKeys.fullPermissions())
            }
            return existing
        }
    }

    fun observeAllStaffUsers(): Flow<List<UserProfile>> = callbackFlow {
        val reg = db.collection("users")
            .whereIn("role", listOf(UserRole.OWNER.name, UserRole.MANAGER.name, UserRole.STAFF.name))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "users")
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(UserProfile::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun createStaffMember(
        name: String,
        email: String,
        phone: String,
        role: UserRole,
        initialPermissions: Map<String, Boolean>
    ): String {
        requireUserId()
        val docRef = db.collection("users").document()
        val staffId = docRef.id
        val data = mapOf(
            "userId" to staffId,
            "name" to name,
            "email" to email,
            "phone" to phone,
            "role" to role.name,
            "status" to "ACTIVE",
            "permissions" to initialPermissions,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(data).await()
        logAudit(
            action = "STAFF_CREATED",
            details = "Created staff: $name ($role) with ${initialPermissions.filterValues { it }.size} permissions",
            targetId = staffId,
            targetType = "USER"
        )
        return staffId
    }

    suspend fun updateStaffPermissions(
        employeeId: String,
        employeeName: String,
        newRole: UserRole,
        oldPermissions: Map<String, Boolean>,
        newPermissions: Map<String, Boolean>
    ) {
        val actorUid = requireUserId()
        val docRef = db.collection("users").document(employeeId)
        docRef.update(
            mapOf(
                "role" to newRole.name,
                "permissions" to newPermissions,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        val changes = newPermissions.mapNotNull { (k, v) ->
            val oldVal = oldPermissions[k] ?: false
            if (oldVal != v) "$k: $oldVal -> $v" else null
        }.joinToString(", ")

        logAudit(
            action = "PERMISSION_CHANGED",
            details = "Changed permissions for $employeeName ($newRole): ${if (changes.isBlank()) "Role updated" else changes}",
            targetId = employeeId,
            targetType = "USER"
        )
    }

    suspend fun logAudit(action: String, details: String, targetId: String = "", targetType: String = "") {
        val currentUser = auth.currentUser
        val actorId = currentUser?.uid ?: "ANONYMOUS"
        val actorEmail = currentUser?.email ?: ""
        val actorName = currentUser?.displayName ?: "User"

        val auditData = mapOf(
            "action" to action,
            "actorId" to actorId,
            "actorName" to actorName,
            "actorEmail" to actorEmail,
            "targetId" to targetId,
            "targetType" to targetType,
            "details" to details,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            db.collection("audit_logs").add(auditData).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "audit_logs")
        }
    }

    fun observeAuditLogs(): Flow<List<AuditLog>> = callbackFlow {
        val reg = db.collection("audit_logs")
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "audit_logs")
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(AuditLog::class.java) } ?: emptyList()
                val sorted = list.sortedByDescending { it.createdAt?.seconds ?: 0L }
                trySend(sorted)
            }
        awaitClose { reg.remove() }
    }
}
