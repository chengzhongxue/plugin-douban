/* tslint:disable */
/* eslint-disable */
/**
 * Halo
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: 2.20.0
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


// May contain unused imports in some cases
// @ts-ignore
import type { CronDouban } from './cron-douban';

/**
 * 
 * @export
 * @interface CronDoubanList
 */
export interface CronDoubanList {
    /**
     * Indicates whether current page is the first page.
     * @type {boolean}
     * @memberof CronDoubanList
     */
    'first': boolean;
    /**
     * Indicates whether current page has previous page.
     * @type {boolean}
     * @memberof CronDoubanList
     */
    'hasNext': boolean;
    /**
     * Indicates whether current page has previous page.
     * @type {boolean}
     * @memberof CronDoubanList
     */
    'hasPrevious': boolean;
    /**
     * A chunk of items.
     * @type {Array<CronDouban>}
     * @memberof CronDoubanList
     */
    'items': Array<CronDouban>;
    /**
     * Indicates whether current page is the last page.
     * @type {boolean}
     * @memberof CronDoubanList
     */
    'last': boolean;
    /**
     * Page number, starts from 1. If not set or equal to 0, it means no pagination.
     * @type {number}
     * @memberof CronDoubanList
     */
    'page': number;
    /**
     * Size of each page. If not set or equal to 0, it means no pagination.
     * @type {number}
     * @memberof CronDoubanList
     */
    'size': number;
    /**
     * Total elements.
     * @type {number}
     * @memberof CronDoubanList
     */
    'total': number;
    /**
     * Indicates total pages.
     * @type {number}
     * @memberof CronDoubanList
     */
    'totalPages': number;
}

