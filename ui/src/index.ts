import { definePlugin } from "@halo-dev/console-shared";
import Douban from "./views/Douban.vue";
import { IconPlug } from "@halo-dev/components";
import { markRaw } from "vue";

export default definePlugin({
  name: "plugin-douban",
  components: {},
  routes: [
    {
      parentName: "Root",
      route: {
        path: "/douban",
        name: "Douban",
        component: Douban,
        meta: {
          title: "豆瓣",
          searchable: true,
          permissions: ["plugin:douban:view"],
          menu: {
            name: "豆瓣",
            group: "content",
            icon: markRaw(IconPlug),
            priority: 0,
          },
        },
      },
    },
  ],
  extensionPoints: {},
});
