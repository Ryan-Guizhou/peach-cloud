<script setup lang="ts">
import { RouterLink } from 'vue-router'
import AuthShell from '../../../components/auth/AuthShell.vue'
</script>

<template>
  <AuthShell :is-typing="false" :show-password="false" :password-length="0">
    <div class="segment">
      <RouterLink to="/login" class="segment__item">Login</RouterLink>
      <RouterLink to="/register" class="segment__item is-active">Register</RouterLink>
    </div>

    <header class="form-header form-header--left">
      <h2>Create account</h2>
      <p>Self-service account registration is not available yet.</p>
    </header>

    <p class="reset-tip" role="status">
      Account creation is managed by your administrator. No registration request can be submitted here.
    </p>

    <p class="return-line">
      Already have an account?
      <RouterLink to="/login" class="forgot-link">Back to login</RouterLink>
    </p>
  </AuthShell>
</template>
