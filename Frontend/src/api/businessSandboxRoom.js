import { request } from './request'

const unwrap = (promise) => promise.then((response) => response.data)

export const createSandboxRoom = (payload) => unwrap(request({
  url: '/api/app/business-sandbox/rooms',
  method: 'POST',
  data: payload,
}))

export const listMySandboxRooms = () => unwrap(request({
  url: '/api/app/business-sandbox/rooms/my',
}))

export const getSandboxRoom = (roomId) => unwrap(request({
  url: `/api/app/business-sandbox/rooms/${roomId}`,
}))

export const createSandboxCompany = (roomId, companyName) => unwrap(request({
  url: `/api/app/business-sandbox/rooms/${roomId}/companies`,
  method: 'POST',
  data: { companyName },
}))

export const joinSandboxCompany = (roomId, companyId, role) => unwrap(request({
  url: `/api/app/business-sandbox/rooms/${roomId}/join`,
  method: 'POST',
  data: { companyId, role },
}))

export const saveSandboxDraft = (roomId, companyId, role, decision) => unwrap(request({
  url: `/api/app/business-sandbox/rooms/${roomId}/companies/${companyId}/draft`,
  method: 'PUT',
  data: { role, decision },
}))

export const confirmSandboxCompany = (roomId, companyId) => unwrap(request({
  url: `/api/app/business-sandbox/rooms/${roomId}/companies/${companyId}/confirm`,
  method: 'POST',
  data: { confirmed: true },
}))

export const startSandboxRoom = (roomId) => unwrap(request({
  url: `/api/app/business-sandbox/rooms/${roomId}/start`,
  method: 'POST',
}))

export const settleSandboxRoom = (roomId) => unwrap(request({
  url: `/api/app/business-sandbox/rooms/${roomId}/settle`,
  method: 'POST',
}))