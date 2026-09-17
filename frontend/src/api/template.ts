import request from './request'

export interface DocTemplate {
  id: number
  templateName: string
  templateType: string
  storedPath: string
  originalName: string
  fileSize: number
  fileFormat: string
  uploadedTime: string
}

export const getTemplates = () => request.get('/templates')

export const uploadTemplate = (file: File, templateName: string, templateType: string) => {
  const fd = new FormData()
  fd.append('file', file)
  fd.append('templateName', templateName)
  fd.append('templateType', templateType)
  return request.post('/templates', fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export const deleteTemplate = (id: number) => request.delete(`/templates/${id}`)

export const exportCompanyDocs = (companyId: number) =>
  request.post('/export/company', { companyId }, { responseType: 'blob' })

export const exportAppointment = (companyId: number, employeeIds: number[], templateId?: number) =>
  request.post('/export/appointment', { companyId, employeeIds, templateId }, { responseType: 'blob' })