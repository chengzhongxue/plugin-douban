<script lang="ts" setup>
import {VButton,VCard,VPageHeader} from "@halo-dev/components";
import {computed, onMounted, ref} from "vue";
import type {CronDouban, CronDoubanList} from "@/types";
import cloneDeep from "lodash.clonedeep";
import apiClient from "@/utils/api-client";

const Se = "cron-default"

const initialFormState: CronDouban = {
  metadata: {
    name: Se,
    creationTimestamp: ""
  },
  spec: {
    cron: "@daily",
    timezone:"Asia/Shanghai",
    suspend: false,
  },
  kind: "CronDouban",
  apiVersion: "douban.moony.la/v1alpha1",
};


const isUpdateMode = computed(() => {
  return !!formState.value.metadata.creationTimestamp;
});

const  saving = ref(false);
const formState = ref<CronDouban>(cloneDeep(initialFormState));
const formSchema = ref(
  [
    {
      $formkit: 'checkbox',
      name: 'suspend',
      label: '是否启用',
      value: false,
    },
    {
      $cmp: 'FormKit',
      props: {
        type: 'select',
        name: 'cron',
        label: '计划',
        options: [
          {value: "@monthly", label: '每月（每月 1 号 0 点）'},
          {value: "@weekly", label: '每周（每周第一天 的 0 点）'},
          {value: "@daily", label: '每天（每天的 0 点）'},
        ],
      }
    },
    {
      $cmp: 'FormKit',
      props: {
        type: 'select',
        name: 'timezone',
        label: '时区',
        options: [
          {value: "Asia/Shanghai", label: 'Asia/Shanghai (GMT+08:00)'},
        ],
      }
    },
  ]
)

const mutate = async () => {
  saving.value = true;
  try {
    if (isUpdateMode.value) {
      const {
        data: data
      } = await apiClient.get(`/apis/douban.moony.la/v1alpha1/crondoubans/${Se}`);
      return formState.value = {
        ...formState.value,
        status: data.status,
        metadata: data.metadata
      },
        await apiClient.put<CronDouban>(
          `/apis/douban.moony.la/v1alpha1/crondoubans/${Se}`,
          formState.value
        );
    } else {
      await apiClient.post<CronDouban>(
        `/apis/douban.moony.la/v1alpha1/crondoubans`,
        formState.value
      );
    }
  } finally {
    saving.value = false;
  }
}

onMounted(async () => {

  const {data: data} = await apiClient.get<CronDoubanList>(`/apis/douban.moony.la/v1alpha1/crondoubans`);
  let items = data.items;
  if (items?.length){
    formState.value = items[0]
  }

});

</script>

<template>
  <VPageHeader title="豆瓣任务">
    <template #actions>
      <VSpace>
        <VButton :route="{ name: 'Douban' }" size="sm">
          返回
        </VButton>
      </VSpace>
    </template>
  </VPageHeader>
  <div class="m-0 md:m-4">
    <VCard :body-class="['!p-0']">
      <Transition mode="out-in" name="fade">
        <div class="bg-white p-4">
          <div>
            <FormKit
              id="cron-setting"
              v-model="formState.spec"
              name="cron-setting"
              :actions="false"
              :preserve="true"
              type="form"
              @submit="mutate"
              submit-label="Login"
            >
              <FormKitSchema :schema="formSchema"/>
            </FormKit>
          </div>
          <div v-permission="['plugin:friends:manage']" class="pt-5">
            <div class="flex justify-start">
              <VButton
                :loading="saving"
                type="secondary"
                @click="$formkit.submit('cron-setting')"
              >
                保存
              </VButton>
            </div>
          </div>
        </div>
      </Transition>
    </VCard>
  </div>

</template>
