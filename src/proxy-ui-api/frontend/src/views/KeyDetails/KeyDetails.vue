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
  <div class="xrd-tab-max-width xrd-view-common">
    <div>
      <subViewTitle
        v-if="key.usage == 'SIGNING'"
        :title="$t('keys.signDetailsTitle')"
        @close="close"
      />
      <subViewTitle
        v-else-if="key.usage == 'AUTHENTICATION'"
        :title="$t('keys.authDetailsTitle')"
        @close="close"
      />
      <subViewTitle v-else :title="$t('keys.detailsTitle')" @close="close" />
      <div class="details-view-tools">
        <large-button
          v-if="canDelete"
          @click="confirmDelete = true"
          :loading="deleting"
          outlined
          >{{ $t('action.delete') }}</large-button
        >
      </div>
    </div>

    <ValidationObserver ref="form" v-slot="{ validate, invalid }">
      <div class="edit-row">
        <div>{{ $t('fields.keys.friendlyName') }}</div>
        <ValidationProvider
          rules="required"
          name="keys.friendlyName"
          v-slot="{ errors }"
          class="validation-provider"
        >
          <v-text-field
            v-model="key.name"
            single-line
            class="code-input"
            name="keys.friendlyName"
            type="text"
            :maxlength="255"
            :error-messages="errors"
            :disabled="!canEdit"
            @input="touched = true"
          ></v-text-field>
        </ValidationProvider>
      </div>

      <div>
        <h3 class="info-title">{{ $t('keys.keyInfo') }}</h3>
        <div class="info-row">
          <div class="row-title">{{ $t('keys.keyId') }}</div>
          <div class="row-data">{{ key.id }}</div>
        </div>
        <div class="info-row">
          <div class="row-title">{{ $t('keys.label') }}</div>
          <div class="row-data">{{ key.label }}</div>
        </div>
        <div class="info-row">
          <div class="row-title">{{ $t('keys.readOnly') }}</div>
          <div class="row-data">{{ key.read_only }}</div>
        </div>
      </div>

      <v-card flat>
        <div class="footer-button-wrap">
          <large-button @click="close()" outlined>{{
            $t('action.cancel')
          }}</large-button>
          <large-button
            class="save-button"
            :loading="saveBusy"
            @click="save()"
            :disabled="!touched || invalid"
            >{{ $t('action.save') }}</large-button
          >
        </div>
      </v-card>
    </ValidationObserver>

    <!-- Confirm dialog delete Key -->
    <confirmDialog
      :dialog="confirmDelete"
      title="keys.deleteTitle"
      text="keys.deleteKeyText"
      @cancel="confirmDelete = false"
      @accept="deleteKey(false)"
    />

    <!-- Warning dialog when key is deleted -->
    <warningDialog
      :dialog="warningDialog"
      :warnings="warningInfo"
      localizationParent="keys"
      @cancel="cancelSubmit()"
      @accept="acceptWarnings()"
    />
  </div>
</template>

<script lang="ts">
/***
 * Component for showing the details of a key
 */
import Vue from 'vue';
import * as api from '@/util/api';
import { ValidationProvider, ValidationObserver } from 'vee-validate';
import { UsageTypes, Permissions, PossibleActions } from '@/global';
import { Key, PossibleActions as PossibleActionsList } from '@/openapi-types';
import SubViewTitle from '@/components/ui/SubViewTitle.vue';
import ConfirmDialog from '@/components/ui/ConfirmDialog.vue';
import LargeButton from '@/components/ui/LargeButton.vue';
import { encodePathParameter } from '@/util/api';
import WarningDialog from '@/components/ui/WarningDialog.vue';

export default Vue.extend({
  components: {
    SubViewTitle,
    ConfirmDialog,
    LargeButton,
    ValidationProvider,
    ValidationObserver,
    WarningDialog,
  },
  props: {
    id: {
      type: String,
      required: true,
    },
  },
  data() {
    return {
      confirmDelete: false,
      touched: false,
      saveBusy: false,
      key: {} as Key,
      possibleActions: [] as string[],
      deleting: false as boolean,
      warningInfo: [] as string[],
      warningDialog: false as boolean,
    };
  },
  computed: {
    canEdit(): boolean {
      if (!this.possibleActions.includes(PossibleActions.EDIT_FRIENDLY_NAME)) {
        return false;
      }

      return this.$store.getters.hasPermission(
        Permissions.EDIT_KEY_FRIENDLY_NAME,
      );
    },
    canDelete(): boolean {
      if (!this.possibleActions.includes(PossibleActions.DELETE)) {
        return false;
      }

      if (this.key.usage === UsageTypes.SIGNING) {
        return this.$store.getters.hasPermission(Permissions.DELETE_SIGN_KEY);
      }

      if (this.key.usage === UsageTypes.AUTHENTICATION) {
        return this.$store.getters.hasPermission(Permissions.DELETE_AUTH_KEY);
      }

      return this.$store.getters.hasPermission(Permissions.DELETE_KEY);
    },
  },
  methods: {
    close(): void {
      this.$router.go(-1);
    },

    save(): void {
      this.saveBusy = true;

      api
        .patch(`/keys/${encodePathParameter(this.id)}`, this.key)
        .then(() => {
          this.saveBusy = false;
          this.$store.dispatch('showSuccess', 'keys.keySaved');
          this.close();
        })
        .catch((error) => {
          this.saveBusy = false;
          this.$store.dispatch('showError', error);
        });
    },

    fetchData(id: string): void {
      api
        .get<Key>(`/keys/${encodePathParameter(id)}`)
        .then((res) => {
          this.key = res.data;
          this.fetchPossibleActions(id);
        })
        .catch((error) => {
          this.$store.dispatch('showError', error);
        });
    },

    fetchPossibleActions(id: string): void {
      api
        .get<PossibleActionsList>(
          `/keys/${encodePathParameter(id)}/possible-actions`,
        )
        .then((res) => {
          this.possibleActions = res.data;
        })
        .catch((error) => {
          this.$store.dispatch('showError', error);
        });
    },
    cancelSubmit(): void {
      this.warningDialog = false;
    },
    acceptWarnings(): void {
      this.warningDialog = false;
      this.deleteKey(true);
    },
    deleteKey(ignoreWarnings: boolean): void {
      this.deleting = true;
      this.confirmDelete = false;

      api
        .remove(
          `/keys/${encodePathParameter(
            this.id,
          )}?ignore_warnings=${ignoreWarnings}`,
        )
        .then(() => {
          this.$store.dispatch('showSuccess', 'keys.keyDeleted');
          this.close();
        })
        .catch((error) => {
          if (error?.response?.data?.warnings) {
            this.warningInfo = error.response.data.warnings;
            this.warningDialog = true;
          } else {
            this.$store.dispatch('showError', error);
          }
        })
        .finally(() => (this.deleting = false));
    },
  },
  created() {
    this.fetchData(this.id);
  },
});
</script>

<style lang="scss" scoped>
@import '../../assets/detail-views';
</style>
