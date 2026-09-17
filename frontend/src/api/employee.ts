import request from './request'

export interface Employee {
  id: number
  companyId: number
  name: string
  gender: string
  idCard: string
  school: string
  major: string
  phone: string
  photo: string
  position: string
  status: string
  createdTime: string
}

export interface Attachment {
  id: number
  employeeId: number
  attachmentType: string
  originalName: string
  storedPath: string
  fileSize: number
  fileFormat: string
  uploadedTime: string
}

export interface EmployeePage {
  records: Employee[]
  total: number
  current: number
  size: number
  pages: number
}

export const getEmployees = (params: {
  companyId?: number
  status?: string
  page?: number
  size?: number
}) => request.get('/employees', { params })

export const getEmployee = (id: number) => request.get(`/employees/${id}`)

export const createEmployee = (data: Partial<Employee>) => request.post('/employees', data)

export const updateEmployee = (id: number, data: Partial<Employee>) => request.put(`/employees/${id}`, data)

export const changeEmployeeStatus = (id: number, data: { status: string }) =>
  request.put(`/employees/${id}/status`, data)

export const deleteEmployee = (id: number) => request.delete(`/employees/${id}`)

export const uploadEmployeePhoto = (id: number, file: File) => {
  const fd = new FormData()
  fd.append('file', file)
  return request.post(`/employees/${id}/photo`, fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export const deleteEmployeePhoto = (id: number) => request.delete(`/employees/${id}/photo`)

export const getAttachments = (employeeId: number) =>
  request.get('/attachments', { params: { employeeId } })

export const uploadAttachment = (employeeId: number, attachmentType: string, file: File) => {
  const fd = new FormData()
  fd.append('employeeId', String(employeeId))
  fd.append('attachmentType', attachmentType)
  fd.append('file', file)
  return request.post('/attachments', fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export const deleteAttachment = (id: number) => request.delete(`/attachments/${id}`)

export const exportRosterExcel = (companyId: number) =>
  request.get('/employees/roster/excel', { params: { companyId }, responseType: 'blob' })

export const exportRosterWord = (companyId: number) =>
  request.get('/employees/roster/word', { params: { companyId }, responseType: 'blob' })