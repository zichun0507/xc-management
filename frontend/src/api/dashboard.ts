import request from './request'

export interface DashboardStats {
  buildingCount: number
  roomTotal: number
  roomFree: number
  roomOccupied: number
  roomDisabled: number
  companyTotal: number
  companyNormal: number
  employeeTotal: number
  employeeActive: number
}

export const getDashboardStats = () => request.get('/dashboard/stats')