// Re-export all API modules
export * from './attendance';
export * from './auth';
export * from './clients';
export * from './performance';

// Export API base URL configuration
export const API_CONFIG = {
	BASE_URL: 'http://localhost:8080/api/v1',

	// Default request headers
	DEFAULT_HEADERS: {
		'Content-Type': 'application/json',
		Accept: 'application/json',
	},

	// Timeout in milliseconds
	TIMEOUT: 30000,

	// Authentication endpoints
	AUTH: {
		REGISTER: '/auth/register',
		LOGIN: '/auth/login',
		REFRESH_TOKEN: '/auth/refresh-token',
		VALIDATE_TOKEN: '/auth/validate-token',
	},

	// Client endpoints
	CLIENTS: {
		BASE: '/clients',
		BY_INSURANCE_TYPE: '/clients/insurance-type',
		BY_POLICY_STATUS: '/clients/policy-status',
		EXPIRING_POLICIES: '/clients/expiring-policies',
	},

	// Attendance endpoints
	ATTENDANCE: {
		BASE: '/attendance',
		CHECK_IN: '/attendance/check-in',
		CHECK_OUT: (id: number) => `/attendance/${id}/check-out`,
		SUMMARY: (year: number, month: number) =>
			`/attendance/summary/${year}/${month}`,
		TEAM: '/attendance/team',
		STATUS: (id: number) => `/attendance/${id}/status`,
	},

	// Performance endpoints
	PERFORMANCE: {
		BASE: '/performance',
		TEAM: '/performance/team',
		FEEDBACK: (id: number) => `/performance/${id}/feedback`,
	},
};

// Export common types
export type ApiResponse<T> = {
	data: T;
	message?: string;
	error?: string;
};

// Export common error types
export class ApiError extends Error {
	constructor(public status: number, message: string, public code?: string) {
		super(message);
		this.name = 'ApiError';
	}
}

// Export utility functions
export const formatDateForApi = (date: Date): string => {
	return date.toISOString().split('T')[0];
};

export const formatDateTimeForApi = (date: Date): string => {
	return date.toISOString();
};

// Export common validation functions
export const isValidEmail = (email: string): boolean => {
	const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
	return emailRegex.test(email);
};

export const isValidPhoneNumber = (phone: string): boolean => {
	const phoneRegex = /^\+?[1-9]\d{1,14}$/;
	return phoneRegex.test(phone);
};

// Export common HTTP request helper
export const fetchWithAuth = async <T>(
	url: string,
	accessToken: string,
	options: RequestInit = {}
): Promise<T> => {
	const response = await fetch(url, {
		...options,
		headers: {
			...API_CONFIG.DEFAULT_HEADERS,
			Authorization: `Bearer ${accessToken}`,
			...options.headers,
		},
	});

	if (!response.ok) {
		const error = await response
			.json()
			.catch(() => ({ message: 'An error occurred' }));
		throw new ApiError(response.status, error.message);
	}

	return response.json();
};
