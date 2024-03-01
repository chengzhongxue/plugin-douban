import {
  type Editor,
  isActive,
  mergeAttributes,
  Node,
  nodeInputRule,
  type Range,
  VueNodeViewRenderer,
  type EditorState,
} from "@halo-dev/richtext-editor";
import DoubanView from "./DoubanView.vue";
import { markRaw } from "vue";
import { ToolboxItem } from "@halo-dev/richtext-editor";
import MdiShare from "~icons/mdi/share";
import BlockActionSeparator from "./BlockActionSeparator.vue";
import MdiDeleteForeverOutline from "~icons/mdi/delete-forever-outline?color=red";
import TablerBrandDouban from '~icons/tabler/brand-douban';
import { deleteNode } from "../utils/delete-node";
declare module "@halo-dev/richtext-editor" {
  interface Commands<ReturnType> {
    douban: {
      setDouban: (options: { src: string }) => ReturnType;
    };
  }
}

const Douban = Node.create({
  name: "douban",

  inline() {
    return true;
  },

  group() {
    return "inline";
  },
  
  addAttributes() {
    return {
      ...this.parent?.(),
      src: {
        default: null,
        parseHTML: (element) => {
          return element.getAttribute("src");
        },
      },
    };
  },

  parseHTML() {
    return [
      {
        tag: "douban",
      },
    ];
  },

  renderHTML({ HTMLAttributes }) {
    return ["douban", mergeAttributes(HTMLAttributes)];
  },

  addCommands() {
    return {
      setDouban:
        (options) =>
          ({ commands }) => {
            return commands.insertContent({
              type: this.name,
              attrs: options,
            });
          },
    };
  },

  addInputRules() {
    return [
      nodeInputRule({
        find: /^\$douban\$$/,
        type: this.type,
        getAttributes: () => {
          return { width: "100%" };
        },
      }),
    ];
  },
  
  addNodeView() {
    return VueNodeViewRenderer(DoubanView);
  },

  addOptions() {
    return {
      getCommandMenuItems() {
        return {
          priority: 2e2,
          icon: markRaw(TablerBrandDouban),
          title: "豆瓣展示",
          keywords: ["douban", "豆瓣展示"],
          command: ({ editor, range }: { editor: Editor; range: Range }) => {
            editor
              .chain()
              .focus()
              .deleteRange(range)
              .insertContent([
                { type: "douban", attrs: { src: "" } },
                { type: "paragraph", content: "" },
              ])
              .run();
          },
        };
      },
      getToolboxItems({ editor }: { editor: Editor }) {
        return {
          priority: 59,
          component: markRaw(ToolboxItem),
          props: {
            editor,
            icon: markRaw(TablerBrandDouban),
            title: "豆瓣展示",
            action: () => {
              editor
                .chain()
                .focus()
                .insertContent([{ type: "douban", attrs: { src: "" } }])
                .run();
            },
          },
        };
      },
      getBubbleMenu({ editor }: { editor: Editor }) {
        return {
          pluginKey: "doubanBubbleMenu",
          shouldShow: ({ state }: { state: EditorState }) => {
            return isActive(state, Douban.name);
          },
          items: [
            {
              priority: 50,
              props: {
                icon: markRaw(MdiShare),
                title: "打开链接",
                action: () => {
                  window.open(editor.getAttributes(Douban.name).src, "_blank");
                },
              },
            },
            {
              priority: 60,
              component: markRaw(BlockActionSeparator),
            },
            {
              priority: 70,
              props: {
                icon: markRaw(MdiDeleteForeverOutline),
                title: "删除",
                action: ({ editor }: { editor: Editor }) => {
                  deleteNode(Douban.name, editor);
                },
              },
            },
          ],
        };
      },
      getDraggable() {
        return {
          getRenderContainer({ dom }: { dom: HTMLElement }) {
            let container = dom;
            while (
              container &&
              !container.hasAttribute("data-node-view-wrapper")
              ) {
              container = container.parentElement as HTMLElement;
            }
            return {
              el: container,
            };
          },
          allowPropagationDownward: true,
        };
      },
    }
  }
  

})
export default Douban;
