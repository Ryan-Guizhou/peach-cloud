<script setup lang="ts">
import { RouterLink } from 'vue-router'
import AuthShell from '../../../components/auth/AuthShell.vue'
</script>

<template>
  <AuthShell :is-typing="false" :show-password="false" :password-length="0">
    <div class="segment">
      <RouterLink to="/login" class="segment__item">Login</RouterLink>
      <RouterLink to="/register" class="segment__item">Register</RouterLink>
    </div>

    <header class="form-header form-header--left">
      <h2>Forgot password</h2>
      <p>Self-service password recovery is not available yet.</p>
    </header>

    <p class="reset-tip" role="status">
      No reset email can be sent from this page. Contact your administrator for account recovery.
    </p>

    <p class="return-line">
      Remembered your password?
      <RouterLink to="/login" class="forgot-link">Back to login</RouterLink>
    </p>
  </AuthShell>
</template>
