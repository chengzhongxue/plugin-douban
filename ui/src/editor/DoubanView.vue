<script lang="ts" setup>
import type { PMNode } from "@halo-dev/richtext-editor";
import type { Editor, Node } from "@halo-dev/richtext-editor";
import { NodeViewWrapper } from "@halo-dev/richtext-editor";
import { computed, onMounted, ref } from "vue";
import {formatDatetime} from "@/utils/date";
import { VButton,VSpace,VDropdown} from "@halo-dev/components";
import {doubanApiClient} from "@/api";
import type { DoubanMovie } from "@/api/generated";

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

const editorLinkObtain = ref();

onMounted(() => {
  if (!src.value) {
  }else {
    handleCheckAllChange();
  }
});

const handleCheckAllChange = async () => {
  const { data: data } = await doubanApiClient.doubanMovie.getDoubanDetail({
    url: src.value
  });
  selecteDoubanMovie.value = data
};

const handleEnterSetExternalLink = () => {
  if (!editorLinkObtain.value) {
    return;
  }
  props.updateAttributes({
    src: editorLinkObtain.value,
  });
};

const handleResetInit = () => {
  props.updateAttributes({src: ""});
};

</script>

<template>
  <node-view-wrapper as="div" class="inline-block-box inline-block">
    <div
      class="inline-block overflow-hidden transition-all text-center relative h-full w-full"
      :class="{
        'rounded ring-2': selected,
      }"
    >
      <div v-if="!src || selecteDoubanMovie?.spec==undefined" class="relative">
        <div class="flex h-64 w-full items-center justify-center" style="height: 180px;">
          <div
            class="flex h-full w-full cursor-pointer flex-col items-center justify-center border-2 border-dashed border-gray-300 bg-gray-50"
          >
            <div
              class="flex flex-col items-center justify-center space-y-7 pb-6 pt-5 editor-link-obtain"
            >
              <VSpace>
                <VDropdown>
                  <div class="flex h-14 w-14 items-center justify-center rounded-full bg-primary/20" style="margin: 0.8em 1.5em;">
                    <svg xmlns="http://www.w3.org/2000/svg" width="1em" height="1em" viewBox="0 0 24 24" style="font-size: 1.6rem;">
                      <path fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 20h16M5 4h14M8 8h8a2 2 0 0 1 2 2v2a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2v-2a2 2 0 0 1 2-2m8 6l-2 6m-6-3l1 3">
                      </path>
                    </svg>
                  </div>
                  <VButton>输入链接</VButton>
                  <template #popper>
                    <input
                      v-model="editorLinkObtain"
                      class="block w-full rounded-md border border-gray-300 bg-gray-50 px-2 py-1.5 text-sm text-gray-900 hover:bg-gray-100"
                      placeholder="输入链接，按回车确定"
                      @keydown.enter="handleEnterSetExternalLink"
                      @change="handleCheckAllChange"
                    />
                  </template>
                </VDropdown>
              </VSpace>
            </div>
          </div>
        </div>
      </div>
      <div v-else class="group relative">
        <div class="doulist-item">
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
                {{selecteDoubanMovie?.faves.remark !=null && selecteDoubanMovie?.faves.remark !='' ? selecteDoubanMovie?.faves.remark : selecteDoubanMovie?.spec.cardSubtitle}}
              </div>
            </div>
          </div>
        </div>
        <div
          v-if="src"
          class="absolute left-0 top-0 hidden h-1/4 w-full cursor-pointer justify-end bg-gradient-to-b from-gray-300 to-transparent p-2 ease-in-out group-hover:flex"
        >
          <VButton size="sm" type="secondary" @click="handleResetInit">
            替换
          </VButton>
        </div>
      </div>
    </div>
  </node-view-wrapper>
</template>

<style>

:root {
  --db--text-color-light: rgba(0, 0, 0, 0.6);
}

.inline-block-box {
  width: calc(100% - 1px);
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
  font-size: 18px;
  display: flex;
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
  max-height: 3.2em;
  display: flex;
}

.doulist-item .doulist-subject img {
  width: 96px!important;
  height: 96px!important;
  border-radius: 4px;
  object-fit: cover
}
.editor-link-obtain .v-popper--theme-dropdown {
  margin-top: -10px;
}
</style>  
