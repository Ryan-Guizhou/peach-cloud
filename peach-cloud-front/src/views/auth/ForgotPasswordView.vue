<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink } from 'vue-router'
import AuthShell from '../../components/auth/AuthShell.vue'

const email = ref('')
const password = ref('')
const isEmailFocused = ref(false)
const showPassword = ref(false)
const passwordLength = computed(() => password.value.length)
const sent = ref(false)

const onSubmit = () => {
  sent.value = true
}
</script>

<template>
  <AuthShell :is-typing="isEmailFocused" :show-password="showPassword" :password-length="passwordLength">
    <div class="segment">
      <RouterLink to="/login" class="segment__item">Login</RouterLink>
      <RouterLink to="/register" class="segment__item">Register</RouterLink>
    </div>

    <header class="form-header form-header--left">
      <h2>Forgot password</h2>
      <p>Submit your email and we will send a secure password reset link.</p>
    </header>

    <form class="login-form" @submit.prevent="onSubmit">
      <label class="field">
        <span>Email</span>
        <div class="field__control">
          <input
            v-model="email"
            type="email"
            placeholder="Enter your email"
            @focus="isEmailFocused = true"
            @blur="isEmailFocused = false"
          />
        </div>
      </label>

      <label class="field">
        <span>Verification password</span>
        <div class="field__control">
          <input
            v-model="password"
            :type="showPassword ? 'text' : 'password'"
            :placeholder="'•'.repeat(8)"
          />
          <button
            type="button"
            class="password-toggle"
            :aria-label="showPassword ? 'Hide password' : 'Show password'"
            @click="showPassword = !showPassword"
          >
            {{ showPassword ? 'Hide' : 'Show' }}
          </button>
        </div>
      </label>

      <button type="submit" class="primary-button">Send reset link</button>
    </form>

    <p v-if="sent" class="reset-tip">If this email exists, a reset link has been sent.</p>

    <p class="return-line">
      Remembered your password?
      <RouterLink to="/login" class="forgot-link">Back to login</RouterLink>
    </p>
  </AuthShell>
</template>
