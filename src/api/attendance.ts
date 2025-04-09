import { API_BASE_URL, createAuthHeaders, handleApiError } from './auth';

// Enums
export enum AttendanceStatus {
	PRESENT = 'PRESENT',
	ABSENT = 'ABSENT',
	LATE = 'LATE',
	HALF_DAY = 'HALF_DAY',
	ON_LEAVE = 'ON_LEAVE',
}

// Types
export interface AttendanceRequest {
	checkInTime: string; // ISO datetime string
	checkOutTime?: string; // ISO datetime string
	status: AttendanceStatus;
	workLocation: string;
	notes?: string;
	isRemoteWork?: boolean;
}

export interface AttendanceSummary {
	totalDaysThisMonth: number;
	presentDays: number;
	lateDays: number;
	halfDays: number;
	absentDays: number;
	leaveDays: number;
	averageHoursWorked: number;
	attendancePercentage: number;
}

export interface AttendanceResponse {
	id: number;

	// Agent information
	agentId: number;
	agentFirstName: string;
	agentLastName: string;
	agentEmail: string;

	// Manager information
	managerId?: number;
	managerFirstName?: string;
	managerLastName?: string;
	managerEmail?: string;

	// Attendance details
	checkInTime: string;
	checkOutTime?: string;
	status: AttendanceStatus;
	workLocation: string;
	notes?: string;
	totalHoursWorked: number;

	// Additional metrics
	isLate: boolean;
	isEarlyCheckout: boolean;
	lateMinutes: number;

	// Timestamps
	createdAt: string;
	updatedAt: string;

	// Summary
	summary: AttendanceSummary;
}

// Helper functions
export const formatDateTime = (date: Date): string => date.toISOString();

export const calculateAttendanceMetrics = (
	attendance: AttendanceResponse
): {
	isLate: boolean;
	lateMinutes: number;
	isEarlyCheckout: boolean;
	totalHoursWorked: number;
} => {
	const checkIn = new Date(attendance.checkInTime);
	const lateThreshold = new Date(checkIn);
	lateThreshold.setHours(6, 30, 0, 0);

	const isLate = checkIn > lateThreshold;
	const lateMinutes = isLate
		? Math.floor((checkIn.getTime() - lateThreshold.getTime()) / 60000)
		: 0;

	let totalHoursWorked = 0;
	let isEarlyCheckout = false;

	if (attendance.checkOutTime) {
		const checkOut = new Date(attendance.checkOutTime);
		totalHoursWorked =
			(checkOut.getTime() - checkIn.getTime()) / (1000 * 60 * 60);
		isEarlyCheckout = totalHoursWorked < 8;
	}

	return {
		isLate,
		lateMinutes,
		isEarlyCheckout,
		totalHoursWorked,
	};
};

// API functions
export const attendanceApi = {
	/**
	 * Get all attendances for the current user
	 * @param accessToken JWT access token
	 * @returns Promise with array of attendances
	 */
	getAll: async (accessToken: string): Promise<AttendanceResponse[]> => {
		const response = await fetch(`${API_BASE_URL}/attendance`, {
			headers: createAuthHeaders(accessToken),
		}).then(handleApiError);

		return response.json();
	},

	/**
	 * Get attendance by ID
	 * @param accessToken JWT access token
	 * @param id Attendance ID
	 * @returns Promise with attendance details
	 */
	getById: async (
		accessToken: string,
		id: number
	): Promise<AttendanceResponse> => {
		const response = await fetch(`${API_BASE_URL}/attendance/${id}`, {
			headers: createAuthHeaders(accessToken),
		}).then(handleApiError);

		return response.json();
	},

	/**
	 * Check in
	 * @param accessToken JWT access token
	 * @param data Check-in data
	 * @returns Promise with created attendance
	 */
	checkIn: async (
		accessToken: string,
		data: AttendanceRequest
	): Promise<AttendanceResponse> => {
		const response = await fetch(`${API_BASE_URL}/attendance/check-in`, {
			method: 'POST',
			headers: createAuthHeaders(accessToken),
			body: JSON.stringify(data),
		}).then(handleApiError);

		return response.json();
	},

	/**
	 * Check out
	 * @param accessToken JWT access token
	 * @param id Attendance ID
	 * @param checkOutTime Check-out time
	 * @returns Promise with updated attendance
	 */
	checkOut: async (
		accessToken: string,
		id: number,
		checkOutTime: string
	): Promise<AttendanceResponse> => {
		const response = await fetch(`${API_BASE_URL}/attendance/${id}/check-out`, {
			method: 'PUT',
			headers: createAuthHeaders(accessToken),
			body: JSON.stringify({ checkOutTime }),
		}).then(handleApiError);

		return response.json();
	},

	/**
	 * Get monthly summary
	 * @param accessToken JWT access token
	 * @param year Year
	 * @param month Month (1-12)
	 * @returns Promise with monthly attendance summary
	 */
	getMonthlySummary: async (
		accessToken: string,
		year: number,
		month: number
	): Promise<AttendanceSummary> => {
		const response = await fetch(
			`${API_BASE_URL}/attendance/summary/${year}/${month}`,
			{
				headers: createAuthHeaders(accessToken),
			}
		).then(handleApiError);

		return response.json();
	},

	/**
	 * Get team attendance (for managers)
	 * @param accessToken JWT access token
	 * @param date Date to get team attendance for
	 * @returns Promise with array of team member attendances
	 */
	getTeamAttendance: async (
		accessToken: string,
		date: string
	): Promise<AttendanceResponse[]> => {
		const response = await fetch(
			`${API_BASE_URL}/attendance/team?date=${date}`,
			{
				headers: createAuthHeaders(accessToken),
			}
		).then(handleApiError);

		return response.json();
	},

	/**
	 * Update attendance status
	 * @param accessToken JWT access token
	 * @param id Attendance ID
	 * @param status New status
	 * @param notes Optional notes
	 * @returns Promise with updated attendance
	 */
	updateStatus: async (
		accessToken: string,
		id: number,
		status: AttendanceStatus,
		notes?: string
	): Promise<AttendanceResponse> => {
		const response = await fetch(`${API_BASE_URL}/attendance/${id}/status`, {
			method: 'PUT',
			headers: createAuthHeaders(accessToken),
			body: JSON.stringify({ status, notes }),
		}).then(handleApiError);

		return response.json();
	},
};
