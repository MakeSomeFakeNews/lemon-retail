import { get } from '@/utils/request'

/** 店长工作台（与 Web 端相同的接口，门店范围由后端控制） */

/** 可管辖、可开单的门店 [{ id, code, name, type, defaultWarehouseId }] */
export const storeOptions = () => get('/store/store/options', { trading: true }, { silent: true })
/** 店长看板：{ storeName, date, today, yesterday: { sales, orders, returns, avgTicket, netSales, cost, grossProfit }, topItems, pendingReceive, draftRequisitions } */
export const storeDashboard = (storeId, date) => get('/chain/dashboard/store', { storeId, date })
