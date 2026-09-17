import request from './request'

export interface SysUser {
  id: number
  username: string
  realName: string
  role: string
  status: string
  createdTime: string
}

export interface OperationLog {
  id: number
  operatorId: number
  operatorName: string
  module: string
  action: string
  content: string
  operationTime: string
}

// users
export const getUsers = (params: { page?: number; size?: number }) =>
  request.get('/users', { params })

export const createUser = (data: { username: string; password: string; realName?: string; role: string }) =>
  request.post('/users', data)

export const updateUserRole = (id: number, role: string) =>
  request.put(`/users/${id}/role`, { role })

export const resetUserPassword = (id: number, password: string) =>
  request.put(`/users/${id}/password`, { password })

// logs
export const getLogs = (params: {
  operatorName?: string
  module?: string
  startTime?: string
  endTime?: string
  page?: number
  size?: number
}) => request.get('/logs', { params })