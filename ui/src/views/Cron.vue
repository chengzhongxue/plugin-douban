<script lang="ts" setup>
import {Toast, VButton, VCard, VPageHeader} from "@halo-dev/components";
import {ref} from "vue";
import type { CronDouban } from "@/api/generated";
import cloneDeep from "lodash.clonedeep";
import {doubanCoreApiClient} from "@/api";
import {useMutation, useQuery} from "@tanstack/vue-query";

const Se = "cron-douban-default"

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

const formState = ref<CronDouban>(cloneDeep(initialFormState));

const {isLoading: cronIsLoading, isFetching: cronIsFetching} = useQuery({
  queryKey: ["cron-douban"],
  queryFn: async () => {
    const {data} = await doubanCoreApiClient.cronDouban.getCronDouban({
      name: Se
    },{
      mute: true
    });
    return data;
  },
  onSuccess(data) {
    formState.value =  data
  },
  retry: false
})

const { mutate:save, isLoading:saveIsLoading } = useMutation({
  mutationKey: ["cron-douban-save"],
  mutationFn: async () => {
    if (formState.value.metadata.creationTimestamp) {
      const { data: data } = await doubanCoreApiClient.cronDouban.getCronDouban({
        name: Se
      });
      formState.value = {
        ...formState.value,
        status: data.status,
        metadata: data.metadata
      };
      return await doubanCoreApiClient.cronDouban.updateCronDouban({
        name: Se,
        cronDouban: formState.value
      });
    }else {
      return await doubanCoreApiClient.cronDouban.createCronDouban({
        cronDouban: formState.value
      });
    }
  },
  onSuccess(data) {
    formState.value = data.data
    Toast.success("保存成功");
  }
});

const cronOptions = [{
  label: "每月（每月 1 号 0 点）",
  value: "@monthly"
}, {
  label: "每周（每周第一天 的 0 点）",
  value: "@weekly"
}, {
  label: "每天（每天的 0 点）",
  value: "@daily"
}, {
  label: "每小时",
  value: "@hourly"
}]

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
              :preserve="true"
              type="form"
              :disabled="cronIsFetching"
              @submit="save"
            >
              <FormKit
                type="checkbox"
                name="suspend"
                label="是否启用"
                value="false"
              />
              <FormKit
                type="select"
                name="cron"
                label="定时表达式"
                allow-create
                searchable
                validatio="required"
                :options="cronOptions"
                help="定时表达式规则请参考：https://docs.spring.io/spring-framework/reference/integration/scheduling.html#scheduling-cron-expression"
              />
              <FormKit
                type="select"
                name="timezone"
                label="时区"
                :options="[
                   {
                     value: 'Asia/Shanghai', 
                     label: 'Asia/Shanghai (GMT+08:00)'
                   },
                ]"
              />
            </FormKit>
          </div>
          <div v-permission="['plugin:douban:manage']" class="pt-5">
            <div class="flex justify-start">
              <VButton
                :loading="saveIsLoading"
                :cronIsLoading="cronIsLoading"
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
