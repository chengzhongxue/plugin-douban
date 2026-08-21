<script lang="ts" setup>
import { Toast, VButton, VModal, VSpace } from "@halo-dev/components";
import { ref, computed, watch, useTemplateRef } from "vue";
import cloneDeep from "lodash.clonedeep";
import { doubanApiClient } from "@/api";
import type { DoubanMovieData } from "@/api/generated";
import { utils } from "@halo-dev/ui-shared";

const props = withDefaults(
  defineProps<{
    doubanMovie?: DoubanMovieData;
  }>(),
  {
    doubanMovie: undefined,
  }
);

const emit = defineEmits<{
  (event: "close"): void;
}>();

const initialFormState: DoubanMovieData = {
  id: undefined,
  creationTimestamp: undefined,
  name: "",
  poster: "",
  link: "",
  doubanId: "",
  score: "",
  year: "",
  type: "",
  pubdate: "",
  cardSubtitle: "",
  dataType: "halo",
  genres: [],
  favesRemark: "",
  favesCreateTime: undefined,
  favesScore: "",
  favesStatus: "done",
};

const formState = ref<DoubanMovieData>(cloneDeep(initialFormState));
const saving = ref(false);
const createTime = ref<string | undefined>(undefined);
const modal = useTemplateRef<InstanceType<typeof VModal> | null>("modal");

const isUpdateMode = computed(() => !!formState.value.creationTimestamp);

const modalTitle = computed(() =>
  isUpdateMode.value ? "编辑条目" : "新建条目"
);

watch(
  () => props.doubanMovie,
  (doubanMovie) => {
    if (doubanMovie) {
      formState.value = cloneDeep(doubanMovie);
      createTime.value = utils.date.toDatetimeLocal(
        formState.value.favesCreateTime
      );
    } else {
      formState.value = cloneDeep(initialFormState);
      createTime.value = undefined;
    }
  },
  { immediate: true }
);

watch(
  () => createTime.value,
  (value) => {
    formState.value.favesCreateTime = value
      ? utils.date.toISOString(value)
      : undefined;
  }
);

const handleSave = async () => {
  try {
    saving.value = true;
    if (isUpdateMode.value) {
      await doubanApiClient.doubanMovie.updateDoubanMovie({
        id: formState.value.id!,
        doubanMovieData: formState.value,
      });
    } else {
      await doubanApiClient.doubanMovie.createDoubanMovie({
        doubanMovieData: formState.value,
      });
    }
    Toast.success("保存成功");
    modal.value?.close();
  } catch (e) {
    console.error(e);
  } finally {
    saving.value = false;
  }
};
</script>

<template>
  <VModal
    ref="modal"
    :title="modalTitle"
    :width="550"
    @close="emit('close')"
  >
    <FormKit
      id="douban-movie-form"
      name="douban-movie-form"
      type="form"
      :config="{ validationVisibility: 'submit' }"
      @submit="handleSave"
    >
      <div class=":uno: mt-5 divide-y divide-gray-100 md:col-span-3 md:mt-0">
        <td v-if="isUpdateMode">
          <p><img :src="formState.poster" width="100" /></p>
          <p>
            {{ formState.name }}
            <span class=":uno: db--titletag">{{
              formState.dataType == "db"
                ? "豆瓣"
                : formState.dataType == "tmdb"
                  ? "TMDB"
                  : "手动添加"
            }}</span>
          </p>
          <p>{{ formState.cardSubtitle }}</p>
        </td>
        <FormKit
          v-if="formState.dataType == 'halo'"
          type="attachment"
          v-model="formState.poster"
          name="poster"
          validation="required"
          label="封面"
        />
        <FormKit
          v-if="formState.dataType == 'halo'"
          type="text"
          v-model="formState.name"
          name="name"
          validation="required"
          label="标题"
        />
        <FormKit
          type="text"
          v-model="formState.link"
          name="link"
          validation="required"
          label="链接"
        />
        <FormKit
          v-if="formState.dataType == 'halo'"
          type="number"
          v-model="formState.score"
          name="score"
          validation="required"
          label="评分"
          max="10"
          min="0"
        />
        <FormKit
          v-if="formState.dataType == 'halo'"
          :options="[
            { label: '电影', value: 'movie' },
            { label: '图书', value: 'book' },
            { label: '音乐', value: 'music' },
            { label: '游戏', value: 'game' },
            { label: '舞台剧', value: 'drama' },
          ]"
          label="类型"
          v-model="formState.type"
          name="type"
          type="select"
        />
        <FormKit
          v-if="formState.dataType == 'halo'"
          type="textarea"
          v-model="formState.cardSubtitle"
          name="cardSubtitle"
          label="描述"
          :rows="4"
          validation="required|length:0,300"
        />
        <FormKit
          type="datetime-local"
          min="0000-01-01T00:00"
          max="9999-12-31T23:59"
          v-model="createTime"
          name="createTime"
          validation="required"
          label="观看时间"
        />
        <FormKit
          :options="[
            { label: '已看', value: 'done' },
            { label: '想看', value: 'mark' },
            { label: '在看', value: 'doing' },
          ]"
          label="状态"
          v-model="formState.favesStatus"
          name="favesStatus"
          type="select"
        />
        <FormKit
          type="textarea"
          v-model="formState.favesRemark"
          name="favesRemark"
          label="我的短评"
          :rows="4"
          validation="length:0,300"
        />
        <FormKit
          type="number"
          v-model="formState.favesScore"
          name="favesScore"
          label="我的评分"
          max="5"
          min="0"
        />
      </div>
    </FormKit>

    <template #footer>
      <VSpace>
        <VButton
          :loading="saving"
          type="secondary"
          @click="$formkit.submit('douban-movie-form')"
        >
          提交
        </VButton>
        <VButton @click="modal?.close()">取消</VButton>
      </VSpace>
    </template>
  </VModal>
</template>

<style lang="scss">
.db--titletag {
  font-size: 13px;
  display: inline-block;
  color: #fff;
  background-color: green;
  border-radius: 2px;
  line-height: 1;
  padding: 2px 3px;
  margin-left: 4px;
}

divide-y td {
  margin-bottom: 9px;
  line-height: 1.3;
  padding-bottom: 1rem;
}

.divide-y td p {
  margin-bottom: 6px;
}
</style>
