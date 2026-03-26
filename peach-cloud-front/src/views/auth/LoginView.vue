<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink } from 'vue-router'
import AuthShell from '../../components/auth/AuthShell.vue'

const loginEmail = ref('')
const loginPassword = ref('')
const isEmailFocused = ref(false)
const showPassword = ref(false)
const remember = ref(true)

const passwordLength = computed(() => loginPassword.value.length)

const onSubmit = () => {
  // Keep behavior placeholder for API integration
}
</script>

<template>
  <AuthShell :is-typing="isEmailFocused" :show-password="showPassword" :password-length="passwordLength">
    <div class="segment">
      <RouterLink to="/login" class="segment__item is-active">Login</RouterLink>
      <RouterLink to="/register" class="segment__item">Register</RouterLink>
    </div>

    <header class="form-header form-header--left">
      <h2>Welcome back</h2>
      <p>Sign in to PeachCloud and continue to your data control panel.</p>
    </header>

    <form class="login-form" @submit.prevent="onSubmit">
      <label class="field">
        <span>Email</span>
        <div class="field__control">
          <input
            v-model="loginEmail"
            type="email"
            placeholder="Enter your email"
            @focus="isEmailFocused = true"
            @blur="isEmailFocused = false"
          />
        </div>
      </label>

      <label class="field">
        <span>Login password</span>
        <div class="field__control">
          <input
            v-model="loginPassword"
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

      <div class="form-row">
        <label class="remember-row">
          <button
            type="button"
            role="checkbox"
            :aria-checked="remember"
            :class="`remember-checkbox${remember ? ' is-checked' : ''}`"
            @click="remember = !remember"
          >
            <span v-if="remember" class="remember-indicator" />
          </button>
          <span>Keep me signed in for 7 days</span>
        </label>
        <RouterLink to="/forgot-password" class="forgot-link">Forgot password?</RouterLink>
      </div>

      <button type="submit" class="primary-button">Login</button>
    </form>
  </AuthShell>
</template>
