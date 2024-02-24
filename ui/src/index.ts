import { definePlugin } from "@halo-dev/console-shared";
import Douban from "./views/Douban.vue";
import Cron from "./views/Cron.vue";
import TablerBrandDouban from '~icons/tabler/brand-douban';
import { markRaw } from "vue";
import { DoubanExtension } from "./editor";

export default definePlugin({
  name: "plugin-douban",
  components: {},
  routes: [
    {
      parentName: "Root",
      route: {
        path: "/douban",
        name: "DoubanRoot",
        meta: {
          title: "豆瓣",
          searchable: true,
          permissions: ["plugin:douban:view"],
          menu: {
            name: "豆瓣",
            group: "content",
            icon: markRaw(TablerBrandDouban),
            priority: 20,
          },
        },
        children: [
          {
            path: "",
            name: "Douban",
            component: Douban,
          },
          {
            path: "cron",
            name: "DoubanCron",
            component: Cron,
            meta: {
              title: "豆瓣任务",
              searchable: true,
              permissions: ["plugin:douban:view"],
            },
          },
        ]
      },
    },
  ],
  extensionPoints: {
    "default:editor:extension:create": () => {
      return [DoubanExtension];
    },
  },
});
