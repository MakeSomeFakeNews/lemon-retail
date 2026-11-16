import request from '@/utils/request'

/** 区域树（登录即可访问）：[{ id, parentId, code, name, sort, status, remark, children }] */
export const regionTree = () => request.get('/store/region/tree')
export const getRegion = (id) => request.get(`/store/region/${id}`)
export const addRegion = (data) => request.post('/store/region', data)
export const updateRegion = (id, data) => request.put(`/store/region/${id}`, data)
export const deleteRegion = (id) => request.delete(`/store/region/${id}`)
