import { API_BASE_URL, createAuthHeaders, handleApiError } from './auth';

// Enums
export enum PerformanceRating {
	OUTSTANDING = 'OUTSTANDING',
	EXCEEDS_EXPECTATIONS = 'EXCEEDS_EXPECTATIONS',
	MEETS_EXPECTATIONS = 'MEETS_EXPECTATIONS',
	NEEDS_IMPROVEMENT = 'NEEDS_IMPROVEMENT',
	UNSATISFACTORY = 'UNSATISFACTORY',
}

// Types
export interface PerformanceRequest {
	periodStart: string; // ISO date
	periodEnd: string; // ISO date
	newClientsAcquired: number;
	policiesRenewed: number;
	totalPremiumCollected: number;
	salesTarget: number;
	salesAchieved: number;
	clientRetentionRate?: number;
	customerSatisfactionScore?: number;
	attendanceScore: number;
	qualityScore: number;
	managerFeedback?: string;
}

export interface PerformanceResponse {
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

	// Performance metrics
	periodStart: string;
	periodEnd: string;
	newClientsAcquired: number;
	policiesRenewed: number;
	totalPremiumCollected: number;
	salesTarget: number;
	salesAchieved: number;
	achievementPercentage: number;
	clientRetentionRate?: number;
	customerSatisfactionScore?: number;
	rating: PerformanceRating;
	managerFeedback?: string;
	attendanceScore: number;
	qualityScore: number;
	overallScore: number;

	// Timestamps
	createdAt: string;
	updatedAt: string;
}

// Helper functions
export const calculatePerformanceMetrics = (
	data: PerformanceRequest
): {
	achievementPercentage: number;
	overallScore: number;
	rating: PerformanceRating;
} => {
	// Calculate achievement percentage
	const achievementPercentage = (data.salesAchieved / data.salesTarget) * 100;

	// Calculate overall score (weighted average)
	const salesWeight = 0.4;
	const retentionWeight = 0.2;
	const satisfactionWeight = 0.2;
	const attendanceWeight = 0.1;
	const qualityWeight = 0.1;

	const salesScore = achievementPercentage;
	const retentionScore = data.clientRetentionRate || 0;
	const satisfactionScore = data.customerSatisfactionScore || 0;
	const attendanceScoreValue = data.attendanceScore;
	const qualityScoreValue = data.qualityScore;

	const overallScore =
		salesScore * salesWeight +
		retentionScore * retentionWeight +
		satisfactionScore * satisfactionWeight +
		attendanceScoreValue * attendanceWeight +
		qualityScoreValue * qualityWeight;

	// Determine rating based on overall score
	let rating: PerformanceRating;
	if (overallScore >= 90) rating = PerformanceRating.OUTSTANDING;
	else if (overallScore >= 80) rating = PerformanceRating.EXCEEDS_EXPECTATIONS;
	else if (overallScore >= 70) rating = PerformanceRating.MEETS_EXPECTATIONS;
	else if (overallScore >= 60) rating = PerformanceRating.NEEDS_IMPROVEMENT;
	else rating = PerformanceRating.UNSATISFACTORY;

	return {
		achievementPercentage,
		overallScore,
		rating,
	};
};

// API functions
export const performanceApi = {
	/**
	 * Get all performances for the current user
	 * @param accessToken JWT access token
	 * @returns Promise with array of performances
	 */
	getAll: async (accessToken: string): Promise<PerformanceResponse[]> => {
		const response = await fetch(`${API_BASE_URL}/performance`, {
			headers: createAuthHeaders(accessToken),
		}).then(handleApiError);

		return response.json();
	},

	/**
	 * Get performance by ID
	 * @param accessToken JWT access token
	 * @param id Performance ID
	 * @returns Promise with performance details
	 */
	getById: async (
		accessToken: string,
		id: number
	): Promise<PerformanceResponse> => {
		const response = await fetch(`${API_BASE_URL}/performance/${id}`, {
			headers: createAuthHeaders(accessToken),
		}).then(handleApiError);

		return response.json();
	},

	/**
	 * Create a new performance record
	 * @param accessToken JWT access token
	 * @param data Performance data
	 * @returns Promise with created performance
	 */
	create: async (
		accessToken: string,
		data: PerformanceRequest
	): Promise<PerformanceResponse> => {
		const response = await fetch(`${API_BASE_URL}/performance`, {
			method: 'POST',
			headers: createAuthHeaders(accessToken),
			body: JSON.stringify(data),
		}).then(handleApiError);

		return response.json();
	},

	/**
	 * Update a performance record
	 * @param accessToken JWT access token
	 * @param id Performance ID
	 * @param data Updated performance data
	 * @returns Promise with updated performance
	 */
	update: async (
		accessToken: string,
		id: number,
		data: Partial<PerformanceRequest>
	): Promise<PerformanceResponse> => {
		const response = await fetch(`${API_BASE_URL}/performance/${id}`, {
			method: 'PUT',
			headers: createAuthHeaders(accessToken),
			body: JSON.stringify(data),
		}).then(handleApiError);

		return response.json();
	},

	/**
	 * Get team performance (for managers)
	 * @param accessToken JWT access token
	 * @param startDate Start date
	 * @param endDate End date
	 * @returns Promise with array of team member performances
	 */
	getTeamPerformance: async (
		accessToken: string,
		startDate: string,
		endDate: string
	): Promise<PerformanceResponse[]> => {
		const response = await fetch(
			`${API_BASE_URL}/performance/team?startDate=${startDate}&endDate=${endDate}`,
			{
				headers: createAuthHeaders(accessToken),
			}
		).then(handleApiError);

		return response.json();
	},

	/**
	 * Add manager feedback
	 * @param accessToken JWT access token
	 * @param id Performance ID
	 * @param feedback Manager feedback
	 * @returns Promise with updated performance
	 */
	addManagerFeedback: async (
		accessToken: string,
		id: number,
		feedback: string
	): Promise<PerformanceResponse> => {
		const response = await fetch(`${API_BASE_URL}/performance/${id}/feedback`, {
			method: 'PUT',
			headers: createAuthHeaders(accessToken),
			body: JSON.stringify({ feedback }),
		}).then(handleApiError);

		return response.json();
	},
};
