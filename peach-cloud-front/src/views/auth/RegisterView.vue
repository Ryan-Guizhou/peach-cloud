<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink } from 'vue-router'
import AuthShell from '../../components/auth/AuthShell.vue'

const registerForm = ref({
  name: '',
  phone: '',
  email: '',
  password: '',
  confirmPassword: '',
})

const isEmailFocused = ref(false)
const showPassword = ref(false)
const passwordLength = computed(() => registerForm.value.password.length)

const onSubmit = () => {
  // Keep behavior placeholder for API integration
}
</script>

<template>
  <AuthShell :is-typing="isEmailFocused" :show-password="showPassword" :password-length="passwordLength">
    <div class="segment">
      <RouterLink to="/login" class="segment__item">Login</RouterLink>
      <RouterLink to="/register" class="segment__item is-active">Register</RouterLink>
    </div>

    <header class="form-header form-header--left">
      <h2>Create account</h2>
      <p>Create your account and start managing workflows and data metrics.</p>
    </header>

    <form class="login-form login-form--register" @submit.prevent="onSubmit">
      <label class="field">
        <span>Username</span>
        <div class="field__control">
          <input v-model="registerForm.name" type="text" placeholder="Enter username" />
        </div>
      </label>

      <div class="grid-two">
        <label class="field">
          <span>Phone</span>
          <div class="field__control">
            <input v-model="registerForm.phone" type="text" placeholder="Enter phone" />
          </div>
        </label>

        <label class="field">
          <span>Email</span>
          <div class="field__control">
            <input
              v-model="registerForm.email"
              type="email"
              placeholder="Enter your email"
              @focus="isEmailFocused = true"
              @blur="isEmailFocused = false"
            />
          </div>
        </label>
      </div>

      <div class="grid-two">
        <label class="field">
          <span>Set password</span>
          <div class="field__control">
            <input
              v-model="registerForm.password"
              :type="showPassword ? 'text' : 'password'"
              :placeholder="'•'.repeat(8)"
            />
          </div>
        </label>

        <label class="field">
          <span>Confirm password</span>
          <div class="field__control">
            <input
              v-model="registerForm.confirmPassword"
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
      </div>

      <button type="submit" class="primary-button">Register</button>
    </form>
  </AuthShell>
</template>
