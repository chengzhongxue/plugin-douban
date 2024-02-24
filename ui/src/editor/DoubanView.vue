<script lang="ts" setup>
import type { PMNode } from "@halo-dev/richtext-editor";
import type { Editor, Node } from "@halo-dev/richtext-editor";
import { NodeViewWrapper } from "@halo-dev/richtext-editor";
import { computed, onMounted, ref } from "vue";
import apiClient from "@/utils/api-client";
import type {DoubanMovie} from "@/types";
import {formatDatetime} from "@/utils/date";

const selecteDoubanMovie = ref<DoubanMovie | undefined>();

const props = defineProps<{
  editor: Editor;
  node: PMNode;
  selected: boolean;
  extension: Node<any, any>;
  getPos: () => number;
  updateAttributes: (attributes: Record<string, any>) => void;
  deleteNode: () => void;
}>();

const src = computed({
  get: () => {
    return props.node?.attrs.src;
  },
  set: (src: string) => {
    props.updateAttributes({ src: src });
  },
});

function handleSetFocus() {
  props.editor.commands.setNodeSelection(props.getPos());
}

const inputRef = ref();

onMounted(() => {
  if (!src.value) {
    inputRef.value.focus();
  }else {
    handleCheckAllChange();
  }
});

const handleCheckAllChange = async () => {
  const { data: data } = await apiClient.get<DoubanMovie>(
    `/apis/api.plugin.halo.run/v1alpha1/plugins/plugin-douban/douban/getDoubanDetail?url=${src.value}`);
  selecteDoubanMovie.value = data
};

</script>

<template>
  <node-view-wrapper as="div" class="inline-block w-full">
    <div
      class="inline-block overflow-hidden transition-all text-center relative h-full w-full"
    >
      <div v-if="!src || selecteDoubanMovie?.spec==undefined" class="p-1.5">
        <input
          ref="inputRef"
          v-model.lazy="src"
          class="block px-2 w-full py-1.5 text-sm text-gray-900 border border-gray-300 rounded-md bg-gray-50 focus:ring-blue-500 focus:border-blue-500"
          placeholder="输入链接，按回车确定"
          tabindex="-1"
          @focus="handleSetFocus"
          @change="handleCheckAllChange"
        />
      </div>
      <div v-else @mouseenter="handleSetFocus" class="doulist-item">
        <div class="doulist-subject">
          <div class="doulist-post"><img decoding="async" referrerpolicy="no-referrer"
                                         :src="selecteDoubanMovie?.spec.poster" class="fade-before fade-after"></div>
          <div class="db--viewTime JiEun">Marked {{formatDatetime(selecteDoubanMovie?.faves.createTime)}}</div>
          <div class="doulist-content">
            <div class="doulist-title"><a :href="selecteDoubanMovie?.spec.link" class="cute" target="_blank"
                                          rel="external nofollow">{{selecteDoubanMovie?.spec.name}}</a></div>
            <div class="rating"><span class="allstardark"><span class="allstarlight" :style="'width:'+selecteDoubanMovie?.spec.score * 10+'%'"></span></span><span
              class="rating_nums"> {{selecteDoubanMovie?.spec.score}} </span></div>
            <div class="abstract">
              {{selecteDoubanMovie?.spec.cardSubtitle}}
            </div>
          </div>
        </div>
      </div>
      
    </div>
  </node-view-wrapper>
</template>

<style>

:root {
  --db--text-color-light: rgba(0, 0, 0, 0.6);
}

.doulist-item {
  margin: 5px 0;
  color: var(--db--text-color-light);
  border: 1px solid #eee;
  border-radius: 4px
}

.doulist-item:hover {
  box-shadow: 0 3px 12px rgba(0,0,0,.05)
}

.doulist-item .doulist-subject {
  display: flex;
  align-items: flex-start;
  line-height: 1.6;
  padding: 12px;
  position: relative
}

.doulist-item .doulist-subject .db--viewTime {
  position: absolute;
  right: 0;
  top: 0;
  background: #f5c518;
  color: #000;
  border-radius: 4px 4px 0 4px;
  line-height: 1;
  padding: 3px 5px 3px 10px;
  font-size: 12px;
  display: flex;
  margin-bottom: 2px;
  font-weight: 900
}

.doulist-item .doulist-subject .db--viewTime:after {
  content: "";
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
  left: 0;
  top: 0;
  margin-left: -.2rem;
  border-radius: 0 4px 4px 4px;
  background: inherit;
  height: 100%;
  position: absolute;
  width: .75rem;
  transform: skewX(20deg)
}

.doulist-item .doulist-subject .doulist-content {
  flex: 1 1 auto
}

.doulist-item .doulist-subject .doulist-post {
  width: 96px;
  margin-right: 15px;
  display: flex;
  flex: 0 0 auto
}

.doulist-item .doulist-subject .doulist-title {
  margin-bottom: 5px;
  font-size: 18px
}

.doulist-item .doulist-subject .doulist-title a {
  text-decoration: none!important
}

.doulist-item .doulist-subject .rating {
  margin: 0 0 5px;
  font-size: 14px;
  line-height: 1;
  display: flex;
  align-items: center
}

.doulist-item .doulist-subject .rating .allstardark {
  position: relative;
  color: #f99b01;
  height: 16px;
  width: 80px;
  background-repeat: repeat;
  background-image: url(/plugins/plugin-douban/assets/static/img/star.svg);
  background-size: auto 100%;
  margin-right: 5px
}

.doulist-item .doulist-subject .rating .allstarlight {
  position: absolute;
  left: 0;
  color: #f99b01;
  height: 16px;
  overflow: hidden;
  background-repeat: repeat;
  background-image: url(/plugins/plugin-douban/assets/static/img/star-fill.svg);
  background-size: auto 100%
}

.doulist-item .doulist-subject .abstract {
  font-size: 14px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.6;
  max-height: 3.2em
}

.doulist-item .doulist-subject img {
  width: 96px!important;
  height: 96px!important;
  border-radius: 4px;
  object-fit: cover
}
</style>  
