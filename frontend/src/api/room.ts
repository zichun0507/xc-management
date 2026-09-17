import request from './request'

export interface Building {
  id: number
  name: string
  sortOrder: number
  landlord: string
  createdTime: string
}

export interface Floor {
  id: number
  buildingId: number
  name: string
  sortOrder: number
}

export interface Room {
  id: number
  buildingId: number
  floorId: number
  roomNumber: string
  status: string
  createdTime: string
}

export interface RoomPage {
  records: Room[]
  total: number
  current: number
  size: number
  pages: number
}

// Building
export const getBuildings = () => request.get('/buildings')
export const createBuilding = (data: { name: string; landlord?: string; sortOrder?: number }) => request.post('/buildings', data)
export const updateBuilding = (id: number, data: { name: string; landlord?: string; sortOrder?: number }) => request.put(`/buildings/${id}`, data)
export const deleteBuilding = (id: number) => request.delete(`/buildings/${id}`)

// Floor
export const getFloors = (buildingId?: number) => request.get('/floors', { params: { buildingId } })
export const createFloor = (data: { buildingId: number; name: string; sortOrder?: number }) => request.post('/floors', data)
export const updateFloor = (id: number, data: { buildingId: number; name: string; sortOrder?: number }) => request.put(`/floors/${id}`, data)
export const deleteFloor = (id: number) => request.delete(`/floors/${id}`)

// Room
export const getRooms = (params: {
  buildingId?: number
  floorId?: number
  roomNumber?: string
  status?: string
  page?: number
  size?: number
}) => request.get('/rooms', { params })

export const createRoom = (data: { buildingId: number; floorId: number; roomNumber: string }) => request.post('/rooms', data)
export const updateRoom = (id: number, data: { buildingId?: number; floorId?: number; roomNumber?: string; status?: string }) => request.put(`/rooms/${id}`, data)
export const deleteRoom = (id: number) => request.delete(`/rooms/${id}`)