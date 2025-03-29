// Base API URL - configure this based on your environment
export const API_BASE_URL = 'http://localhost:8080/api/v1';

// Types for authentication
export interface AuthRequest {
	email: string;
	password: string;
}

export interface RegisterRequest {
	firstName: string;
	lastName: string;
	email: string;
	password: string;
	phoneNumber: string;
}

export interface Role {
	id: number;
	name: string;
}

export interface AuthResponse {
	accessToken: string;
	refreshToken: string;
	tokenType: string;
	expiresIn: number;
	email: string;
	firstName: string;
	lastName: string;
	roles: Role[];
	message: string;
	type: string;
}

// Authentication API functions
export const authApi = {
	/**
	 * Register a new user
	 * @param data Registration data
	 * @returns Promise with auth tokens and user info
	 */
	register: async (data: RegisterRequest): Promise<AuthResponse> => {
		const response = await fetch(`${API_BASE_URL}/auth/register`, {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json',
			},
			body: JSON.stringify(data),
		});

		if (!response.ok) {
			throw new Error('Registration failed');
		}

		return response.json();
	},

	/**
	 * Login with email and password
	 * @param data Login credentials
	 * @returns Promise with auth tokens and user info
	 */
	login: async (data: AuthRequest): Promise<AuthResponse> => {
		const response = await fetch(`${API_BASE_URL}/auth/login`, {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json',
			},
			body: JSON.stringify(data),
		});

		if (!response.ok) {
			throw new Error('Login failed');
		}

		return response.json();
	},

	/**
	 * Refresh the access token using a refresh token
	 * @param refreshToken Current refresh token
	 * @returns Promise with new auth tokens
	 */
	refreshToken: async (refreshToken: string): Promise<AuthResponse> => {
		const response = await fetch(`${API_BASE_URL}/auth/refresh-token`, {
			method: 'POST',
			headers: {
				Authorization: `Bearer ${refreshToken}`,
			},
		});

		if (!response.ok) {
			throw new Error('Token refresh failed');
		}

		return response.json();
	},

	/**
	 * Validate an access token
	 * @param accessToken Current access token
	 * @returns Promise<boolean> indicating if token is valid
	 */
	validateToken: async (accessToken: string): Promise<boolean> => {
		const response = await fetch(`${API_BASE_URL}/auth/validate-token`, {
			method: 'GET',
			headers: {
				Authorization: `Bearer ${accessToken}`,
			},
		});

		return response.ok;
	},
};

// Helper function to create headers with authentication
export const createAuthHeaders = (accessToken: string): HeadersInit => ({
	Authorization: `Bearer ${accessToken}`,
	'Content-Type': 'application/json',
});

// Helper function to handle API errors
export class ApiError extends Error {
	constructor(public status: number, message: string) {
		super(message);
		this.name = 'ApiError';
	}
}

export const handleApiError = async (response: Response) => {
	if (!response.ok) {
		const error = await response
			.json()
			.catch(() => ({ message: 'An error occurred' }));
		throw new ApiError(response.status, error.message);
	}
	return response;
};
