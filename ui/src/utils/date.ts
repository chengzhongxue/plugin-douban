import dayjs from "dayjs";
import "dayjs/locale/zh-cn";
import timezone from "dayjs/plugin/timezone";
import TimeAgo from "javascript-time-ago";
import utc from "dayjs/plugin/utc";
import zh from "javascript-time-ago/locale/zh";

dayjs.extend(timezone);
dayjs.extend(utc);
dayjs.locale("zh-cn");

TimeAgo.addDefaultLocale(zh);

export function formatDatetime(date: string | Date | undefined | null): string {
  if (!date) {
    return "";
  }
  return dayjs(date).format("YYYY-MM-DD HH:mm");
}

export function timeAgo(
  date: string | Date | number | undefined | null
): string {
  if (!date) {
    return "";
  }

  const currentDate = new Date();
  const inputDate = new Date(date);

  // 365天 * 24小时 * 60分钟 * 60秒 * 1000毫秒
  const oneYearInMilliseconds = 365 * 24 * 60 * 60 * 1000;

  if (currentDate.getTime() - inputDate.getTime() > oneYearInMilliseconds) {
    return dayjs(date).format("YYYY-MM-DD");
  }

  const timeAgo = new TimeAgo("zh");

  return timeAgo.format(new Date(date));
}

export function toDatetimeLocal(date: string | Date | undefined | null): string {
  if (!date) {
    return "";
  }
  return dayjs(date).format("YYYY-MM-DDTHH:mm");
}

export function toISOString(date: string | Date | undefined | null): string {
  if (!date) {
    return "";
  }
  return dayjs(date).utc(false).toISOString();
}

