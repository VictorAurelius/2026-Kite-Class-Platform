// Case B fixture — same FE calls as Case A.
import { apiClient } from "./client";

export const listClasses = () => apiClient.get("/api/v1/classes");
export const getClass = (id: string) => apiClient.get(`/api/v1/classes/${id}`);
