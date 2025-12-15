import { definePlugin } from "@halo-dev/ui-shared";
import TablerBrandDouban from '~icons/tabler/brand-douban';
import { markRaw } from "vue";
import 'uno.css';

export default definePlugin({
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
            component: () => import('./views/Douban.vue'),
          },
          {
            path: "cron",
            name: "DoubanCron",
            component: () => import('./views/Cron.vue'),
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
    'default:editor:extension:create': async () => {
      const { DoubanExtension } = await import('./editor');
      return [DoubanExtension];
    },
  },
});
