/**
 * app.js - Main JavaScript file for CV Generator
 *
 * Provides client-side interactivity for:
 * - Form validation
 * - AJAX requests
 * - Progress updates
 * - UI feedback
 *
 * @author Pranav Ghorpade
 */

// =============================================================================
// CONFIGURATION
// =============================================================================

const CONFIG = {
    // API endpoints
    API_GENERATE: '/api/generate',
    API_STATUS: '/api/status/',
    API_RESULT: '/api/result/',

    // Polling settings
    POLL_INTERVAL: 2000,    // 2 seconds
    MAX_POLL_ATTEMPTS: 60,  // 2 minutes max

    // Validation
    MIN_JD_LENGTH: 50,
    MAX_JD_LENGTH: 50000
};

// =============================================================================
// UTILITY FUNCTIONS
// =============================================================================

/**
 * Makes a POST request to the API.
 *
 * @param {string} url - API endpoint
 * @param {object} data - Request body
 * @returns {Promise<object>} Response data
 */
async function postJson(url, data) {
    const response = await fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
    });

    if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.errorMessage || `HTTP ${response.status}`);
    }

    return response.json();
}

/**
 * Makes a GET request to the API.
 *
 * @param {string} url - API endpoint
 * @returns {Promise<object>} Response data
 */
async function getJson(url) {
    const response = await fetch(url);

    if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.errorMessage || `HTTP ${response.status}`);
    }

    return response.json();
}

/**
 * Shows a notification message to the user.
 *
 * @param {string} message - Message to display
 * @param {string} type - 'success', 'error', or 'info'
 */
function showNotification(message, type = 'info') {
    // Create notification element
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.textContent = message;

    // Add to page
    document.body.appendChild(notification);

    // Animate in
    setTimeout(() => notification.classList.add('show'), 10);

    // Remove after 5 seconds
    setTimeout(() => {
        notification.classList.remove('show');
        setTimeout(() => notification.remove(), 300);
    }, 5000);
}

/**
 * Validates job description input.
 *
 * @param {string} text - Job description text
 * @returns {object} Validation result {valid: boolean, message: string}
 */
function validateJobDescription(text) {
    if (!text || text.trim().length === 0) {
        return { valid: false, message: 'Job description is required' };
    }

    if (text.trim().length < CONFIG.MIN_JD_LENGTH) {
        return {
            valid: false,
            message: `Job description must be at least ${CONFIG.MIN_JD_LENGTH} characters`
        };
    }

    if (text.length > CONFIG.MAX_JD_LENGTH) {
        return {
            valid: false,
            message: `Job description must be less than ${CONFIG.MAX_JD_LENGTH} characters`
        };
    }

    return { valid: true };
}

// =============================================================================
// CV GENERATION
// =============================================================================

/**
 * CvGenerator class handles the CV generation flow.
 */
class CvGenerator {
    constructor() {
        this.jobId = null;
        this.pollCount = 0;
        this.pollInterval = null;
    }

    /**
     * Starts the CV generation process.
     *
     * @param {string} jobDescription - The JD text
     * @param {string} companyName - Optional company name
     */
    async generate(jobDescription, companyName) {
        // Validate input
        const validation = validateJobDescription(jobDescription);
        if (!validation.valid) {
            showNotification(validation.message, 'error');
            return;
        }

        try {
            // Submit generation request
            const response = await postJson(CONFIG.API_GENERATE, {
                jobDescription,
                companyName: companyName || null
            });

            this.jobId = response.id;

            // Check if it was a cache hit (already completed)
            if (response.status === 'COMPLETED') {
                window.location.href = `/result/${this.jobId}`;
                return;
            }

            // Redirect to generating page
            window.location.href = `/generating/${this.jobId}`;

        } catch (error) {
            showNotification(`Generation failed: ${error.message}`, 'error');
        }
    }

    /**
     * Starts polling for generation status.
     *
     * @param {string} jobId - The generation job ID
     * @param {function} onProgress - Callback for progress updates
     * @param {function} onComplete - Callback when generation completes
     * @param {function} onError - Callback for errors
     */
    startPolling(jobId, onProgress, onComplete, onError) {
        this.jobId = jobId;
        this.pollCount = 0;

        this.pollInterval = setInterval(async () => {
            this.pollCount++;

            if (this.pollCount > CONFIG.MAX_POLL_ATTEMPTS) {
                this.stopPolling();
                onError('Generation timed out. Please try again.');
                return;
            }

            try {
                const status = await getJson(CONFIG.API_STATUS + this.jobId);

                if (status.status === 'COMPLETED') {
                    this.stopPolling();
                    onComplete(status);
                } else if (status.status === 'FAILED') {
                    this.stopPolling();
                    onError(status.errorMessage || 'Generation failed');
                } else {
                    // Update progress
                    const progress = status.progress || this.estimateProgress();
                    onProgress(progress, status.currentStep);
                }

            } catch (error) {
                console.error('Status check failed:', error);
                // Don't stop polling on temporary errors
            }

        }, CONFIG.POLL_INTERVAL);
    }

    /**
     * Stops polling for status.
     */
    stopPolling() {
        if (this.pollInterval) {
            clearInterval(this.pollInterval);
            this.pollInterval = null;
        }
    }

    /**
     * Estimates progress based on elapsed poll count.
     *
     * @returns {number} Estimated progress percentage
     */
    estimateProgress() {
        const elapsed = this.pollCount * (CONFIG.POLL_INTERVAL / 1000);

        if (elapsed < 5) return Math.round(10 + elapsed * 4);
        if (elapsed < 15) return Math.round(30 + (elapsed - 5) * 4);
        if (elapsed < 25) return Math.round(70 + (elapsed - 15) * 2);
        return Math.min(99, 90 + Math.round(elapsed - 25));
    }
}

// =============================================================================
// UI COMPONENTS
// =============================================================================

/**
 * Character counter for textarea.
 *
 * @param {HTMLTextAreaElement} textarea - The textarea element
 * @param {HTMLElement} counter - The counter display element
 */
function initCharacterCounter(textarea, counter) {
    if (!textarea || !counter) return;

    const updateCount = () => {
        const count = textarea.value.length;
        counter.textContent = count.toLocaleString();

        // Add visual feedback for length
        if (count < CONFIG.MIN_JD_LENGTH) {
            counter.classList.add('warning');
            counter.classList.remove('success');
        } else if (count > CONFIG.MAX_JD_LENGTH * 0.9) {
            counter.classList.add('warning');
            counter.classList.remove('success');
        } else {
            counter.classList.add('success');
            counter.classList.remove('warning');
        }
    };

    textarea.addEventListener('input', updateCount);
    updateCount(); // Initial count
}

/**
 * Progress bar updater.
 *
 * @param {number} progress - Progress percentage (0-100)
 * @param {HTMLElement} fillElement - The progress fill element
 * @param {HTMLElement} textElement - The progress text element
 */
function updateProgressBar(progress, fillElement, textElement) {
    if (fillElement) {
        fillElement.style.width = `${progress}%`;
    }
    if (textElement) {
        textElement.textContent = `${progress}%`;
    }
}

// =============================================================================
// INITIALIZATION
// =============================================================================

// Create global generator instance
window.cvGenerator = new CvGenerator();

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', () => {
    // Initialize character counter if present
    const textarea = document.getElementById('jobDescription');
    const charCount = document.getElementById('charCount');
    initCharacterCounter(textarea, charCount);

    // Log initialization
    console.log('CV Generator initialized');
});

// =============================================================================
// CSS FOR NOTIFICATIONS (injected)
// =============================================================================

const notificationStyles = document.createElement('style');
notificationStyles.textContent = `
    .notification {
        position: fixed;
        bottom: 20px;
        right: 20px;
        padding: 12px 24px;
        border-radius: 8px;
        color: white;
        font-weight: 500;
        opacity: 0;
        transform: translateY(20px);
        transition: all 0.3s ease;
        z-index: 1000;
        max-width: 400px;
    }

    .notification.show {
        opacity: 1;
        transform: translateY(0);
    }

    .notification-success {
        background: #22c55e;
    }

    .notification-error {
        background: #ef4444;
    }

    .notification-info {
        background: #2563eb;
    }

    #charCount.warning {
        color: #f59e0b;
    }

    #charCount.success {
        color: #22c55e;
    }
`;
document.head.appendChild(notificationStyles);
