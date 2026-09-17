import request from './request'

export interface Company {
  id: number
  companyName: string
  shortName: string
  englishName: string
  unifiedCode: string
  legalPerson: string
  contactPerson: string
  contactPhone: string
  address: string
  businessStatus: string
  leaseStartDate: string
  leaseEndDate: string
  remark: string
  createdTime: string
}

export interface CompanyPage {
  records: Company[]
  total: number
  current: number
  size: number
  pages: number
}

export interface ImportResult {
  successCount: number
  totalCount: number
  errors: { row: number; reason: string }[]
}

// list
export const getCompanies = (params: {
  companyName?: string
  roomNumber?: string
  buildingId?: number
  floorId?: number
  page?: number
  size?: number
}) => request.get('/companies', { params })

// detail
export const getCompanyDetail = (id: number) => request.get(`/companies/${id}`)

// create
export const createCompany = (data: {
  companyName: string
  unifiedCode?: string
  legalPerson?: string
  contactPerson?: string
  contactPhone?: string
  address?: string
  remark?: string
  roomIds?: number[]
}) => request.post('/companies', data)

// update
export const updateCompany = (id: number, data: {
  companyName?: string
  unifiedCode?: string
  legalPerson?: string
  contactPerson?: string
  contactPhone?: string
  address?: string
  remark?: string
  roomIds?: number[]
}) => request.put(`/companies/${id}`, data)

// status change
export const changeCompanyStatus = (id: number, data: { businessStatus: string; remark: string }) =>
  request.put(`/companies/${id}/status`, data)

// export
export const exportCompanies = (params: {
  companyName?: string
  roomNumber?: string
  buildingId?: number
  floorId?: number
}) => request.get('/companies/export', { params, responseType: 'blob' })

// import
export const importCompanies = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/companies/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

// template
export const downloadTemplate = () =>
  request.get('/companies/template', { responseType: 'blob' })