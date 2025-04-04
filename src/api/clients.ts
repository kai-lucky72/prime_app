import { API_BASE_URL, createAuthHeaders, handleApiError } from './auth';

// Enums
export enum InsuranceType {
	LIFE = 'LIFE',
	HEALTH = 'HEALTH',
	AUTO = 'AUTO',
	PROPERTY = 'PROPERTY',
	BUSINESS = 'BUSINESS',
}

export enum PolicyStatus {
	ACTIVE = 'ACTIVE',
	PENDING = 'PENDING',
	EXPIRED = 'EXPIRED',
	CANCELLED = 'CANCELLED',
	RENEWED = 'RENEWED',
}

// Types
export interface ClientRequest {
	name: string;
	nationalId: string;
	email?: string;
	phoneNumber: string;
	address?: string;
	location: string;
	insuranceType: InsuranceType;
	policyNumber?: string;
	policyStartDate?: string; // ISO date string
	policyEndDate?: string; // ISO date string
	premiumAmount?: number;
	policyStatus?: PolicyStatus;
}

export interface ClientResponse {
	id: number;
	name: string;
	nationalId: string;
	email?: string;
	phoneNumber: string;
	address?: string;
	location: string;
	insuranceType: InsuranceType;
	policyNumber?: string;
	policyStartDate?: string;
	policyEndDate?: string;
	premiumAmount?: number;
	policyStatus?: PolicyStatus;

	// Agent information
	agentId: number;
	agentFirstName: string;
	agentLastName: string;
	agentEmail: string;

	// Policy metrics
	daysUntilExpiration?: number;
	isExpiringSoon?: boolean;
	totalPremiumsPaid?: number;
	yearsAsClient?: number;
	needsRenewal?: boolean;

	// Timestamps
	createdAt: string;
	updatedAt: string;
}

// Helper functions
export const formatDate = (date: Date): string =>
	date.toISOString().split('T')[0];

export const calculatePolicyStatus = (
	client: ClientResponse
): {
	isActive: boolean;
	isExpired: boolean;
	daysRemaining: number;
	needsRenewal: boolean;
} => {
	const today = new Date();
	const endDate = new Date(client.policyEndDate);
	const daysRemaining = Math.ceil(
		(endDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24)
	);

	return {
		isActive: client.policyStatus === PolicyStatus.ACTIVE,
		isExpired: endDate < today,
		daysRemaining,
		needsRenewal:
			daysRemaining <= 30 && client.policyStatus === PolicyStatus.ACTIVE,
	};
};

// API functions
export const clientsApi = {
	/**
	 * Get all clients
	 * @param accessToken JWT access token
	 * @returns Promise with array of clients
	 */
	getAll: async (accessToken: string): Promise<ClientResponse[]> => {
		const response = await fetch(`${API_BASE_URL}/clients`, {
			headers: createAuthHeaders(accessToken),
		}).then(handleApiError);

		return response.json();
	},

	/**
	 * Get a client by ID
	 * @param accessToken JWT access token
	 * @param id Client ID
	 * @returns Promise with client details
	 */
	getById: async (accessToken: string, id: number): Promise<ClientResponse> => {
		const response = await fetch(`${API_BASE_URL}/clients/${id}`, {
			headers: createAuthHeaders(accessToken),
		}).then(handleApiError);

		return response.json();
	},

	/**
	 * Create a new client
	 * @param accessToken JWT access token
	 * @param data Client data
	 * @returns Promise with created client
	 */
	create: async (
		accessToken: string,
		data: ClientRequest
	): Promise<ClientResponse> => {
		const response = await fetch(`${API_BASE_URL}/clients`, {
			method: 'POST',
			headers: createAuthHeaders(accessToken),
			body: JSON.stringify(data),
		}).then(handleApiError);

		return response.json();
	},

	/**
	 * Update a client
	 * @param accessToken JWT access token
	 * @param id Client ID
	 * @param data Updated client data
	 * @returns Promise with updated client
	 */
	update: async (
		accessToken: string,
		id: number,
		data: Partial<ClientRequest>
	): Promise<ClientResponse> => {
		const response = await fetch(`${API_BASE_URL}/clients/${id}`, {
			method: 'PUT',
			headers: createAuthHeaders(accessToken),
			body: JSON.stringify(data),
		}).then(handleApiError);

		return response.json();
	},

	/**
	 * Delete a client
	 * @param accessToken JWT access token
	 * @param id Client ID
	 * @returns Promise<void>
	 */
	delete: async (accessToken: string, id: number): Promise<void> => {
		await fetch(`${API_BASE_URL}/clients/${id}`, {
			method: 'DELETE',
			headers: createAuthHeaders(accessToken),
		}).then(handleApiError);
	},

	/**
	 * Get clients by insurance type
	 * @param accessToken JWT access token
	 * @param type Insurance type
	 * @returns Promise with array of clients
	 */
	getByInsuranceType: async (
		accessToken: string,
		type: InsuranceType
	): Promise<ClientResponse[]> => {
		const response = await fetch(
			`${API_BASE_URL}/clients/insurance-type/${type}`,
			{
				headers: createAuthHeaders(accessToken),
			}
		).then(handleApiError);

		return response.json();
	},

	/**
	 * Get clients by policy status
	 * @param accessToken JWT access token
	 * @param status Policy status
	 * @returns Promise with array of clients
	 */
	getByPolicyStatus: async (
		accessToken: string,
		status: PolicyStatus
	): Promise<ClientResponse[]> => {
		const response = await fetch(
			`${API_BASE_URL}/clients/policy-status/${status}`,
			{
				headers: createAuthHeaders(accessToken),
			}
		).then(handleApiError);

		return response.json();
	},

	/**
	 * Get expiring policies (within 30 days)
	 * @param accessToken JWT access token
	 * @returns Promise with array of clients with expiring policies
	 */
	getExpiringPolicies: async (
		accessToken: string
	): Promise<ClientResponse[]> => {
		const response = await fetch(`${API_BASE_URL}/clients/expiring-policies`, {
			headers: createAuthHeaders(accessToken),
		}).then(handleApiError);

		return response.json();
	},
};
