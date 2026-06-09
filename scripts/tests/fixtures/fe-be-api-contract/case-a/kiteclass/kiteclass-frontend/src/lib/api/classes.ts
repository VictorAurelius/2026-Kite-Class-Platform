// Case A fixture — FE gọi collection GET /api/v1/classes (pre-GAP-1069 state).
import { apiClient } from "./client";

export const listClasses = () => apiClient.get("/api/v1/classes");
export const getClass = (id: string) => apiClient.get(`/api/v1/classes/${id}`);
