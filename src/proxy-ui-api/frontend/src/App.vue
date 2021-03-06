<!--
   The MIT License
   Copyright (c) 2019- Nordic Institute for Interoperability Solutions (NIIS)
   Copyright (c) 2018 Estonian Information System Authority (RIA),
   Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
   Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)

   Permission is hereby granted, free of charge, to any person obtaining a copy
   of this software and associated documentation files (the "Software"), to deal
   in the Software without restriction, including without limitation the rights
   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
   copies of the Software, and to permit persons to whom the Software is
   furnished to do so, subject to the following conditions:

   The above copyright notice and this permission notice shall be included in
   all copies or substantial portions of the Software.

   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
   THE SOFTWARE.
 -->
<template>
  <v-app class="xrd-app">
    <app-toolbar />
    <v-content app>
      <alerts-container />
      <transition name="fade" mode="out-in">
        <router-view />
      </transition>
    </v-content>
    <snackbar />
    <app-footer />
  </v-app>
</template>

<script lang="ts">
import Vue from 'vue';
import axios from 'axios';
import Snackbar from '@/components/ui/Snackbar.vue';
import { RouteName } from '@/global';
import AppFooter from '@/components/layout/AppFooter.vue';
import AppToolbar from '@/components/layout/AppToolbar.vue';
import AlertsContainer from '@/components/ui/AlertsContainer.vue';

export default Vue.extend({
  name: 'App',
  components: {
    AppToolbar,
    AppFooter,
    Snackbar,
    AlertsContainer,
  },
  created() {
    // Add a response interceptor
    axios.interceptors.response.use(
      (response) => {
        return response;
      },
      (error) => {
        // Check that it's proper "unauthorized error".
        // Also the response from from session timeout polling is handled elsewhere
        if (
          error.response.status === 401 &&
          error.response.config &&
          !error.response.config.__isRetryRequest &&
          !error.request.responseURL.includes('notifications/session-status')
        ) {
          // if you ever get an unauthorized, logout the user
          this.$store.dispatch('clearAuth');
          this.$store.dispatch('clearAlerts');
          this.$router.replace({ name: RouteName.Login });
        }

        // If the request is made with responseType: blob, but backend responds with json error
        if (
          error.request.responseType === 'blob' &&
          error.response.data instanceof Blob &&
          error.response.data.type &&
          error.response.data.type.toLowerCase().indexOf('json') != -1
        ) {
          return new Promise((resolve, reject) => {
            const reader = new FileReader();

            reader.onload = () => {
              error.response.data = JSON.parse(reader.result as string);
              resolve(Promise.reject(error));
            };

            reader.onerror = () => {
              reject(error);
            };

            reader.readAsText(error.response.data);
          });
        }

        // Do something with response error
        return Promise.reject(error);
      },
    );
  },
});
</script>

<style lang="scss">
@import './assets/global-style.scss';
</style>

<style lang="scss" scoped>
.fade-enter-active,
.fade-leave-active {
  transition-duration: 0.2s;
  transition-property: opacity;
  transition-timing-function: ease;
}

.fade-enter,
.fade-leave-active {
  opacity: 0;
}

// Set the app background color
.theme--light.v-application.xrd-app {
  background: white;
}
</style>
