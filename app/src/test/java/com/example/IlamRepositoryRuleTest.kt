package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Product
import com.example.data.model.UserRole
import com.example.data.repository.AuthAndUserRepository
import com.example.data.repository.ProductAndCategoryRepository
import com.example.data.repository.SaleAndOrderRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class IlamRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun publicCategories_canBeObservedWithoutAuth() {
    runBlocking {
      auth.signOut()
      val repo = ProductAndCategoryRepository(firestore)
      val categories = withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.observeCategories().first()
      }
      assertNotNull(categories)
    }
  }

  @Test
  fun addProduct_authenticatedOwner_createsProductSuccessfully() {
    runBlocking {
      val ownerEmail = "hk3156140@gmail.com"
      val uid = signInTestUser(ownerEmail)

      // Setup Owner role in DB
      val userRepo = AuthAndUserRepository(firestore)
      firestore.collection("users").document(uid).set(
        mapOf(
          "userId" to uid,
          "name" to "Store Owner",
          "email" to ownerEmail,
          "role" to UserRole.OWNER.name,
          "status" to "ACTIVE"
        )
      ).await()

      val productRepo = ProductAndCategoryRepository(firestore)
      val productId = productRepo.addProduct(
        Product(
          name = "Signature Linen Blazer",
          sku = "ILAM-BLZ-001",
          category = "MEN",
          sellingPrice = 7500.0,
          currentStock = 12
        )
      )

      assertTrue(productId.isNotEmpty())
      val fetched = productRepo.getProductById(productId)
      assertEquals("Signature Linen Blazer", fetched?.name)
    }
  }

  @Test
  fun unauthenticatedUser_cannotReadSales() {
    runBlocking {
      auth.signOut()
      val saleRepo = SaleAndOrderRepository(firestore)
      try {
        withTimeout(FLOW_TIMEOUT_MS) {
          saleRepo.observeSales().first()
        }
        fail("Expected FirebaseFirestoreException PERMISSION_DENIED")
      } catch (e: FirebaseFirestoreException) {
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
      } catch (e: Exception) {
        // Can be wrapped exception
        assertTrue(e.message?.contains("PERMISSION_DENIED") == true || e.cause is FirebaseFirestoreException)
      }
    }
  }

  private companion object {
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
