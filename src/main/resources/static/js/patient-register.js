document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('patient-registration-form');
    const messageDiv = document.getElementById('registration-message');
    const passwordInput = document.getElementById('password');
    const confirmPasswordInput = document.getElementById('confirmPassword');
    const passwordMatchError = document.getElementById('password-match-error');

    // Function to display messages
    const showMessage = (message, type = 'danger') => {
        messageDiv.textContent = message;
        messageDiv.className = `alert alert-${type}`; // Use Bootstrap alert classes
        messageDiv.classList.remove('d-none'); // Make it visible
    };

    // Function to hide messages
    const hideMessage = () => {
        messageDiv.classList.add('d-none');
        messageDiv.className = 'alert d-none';
    };

    // Basic client-side password match check
    const checkPasswordMatch = () => {
        if (passwordInput.value !== confirmPasswordInput.value && confirmPasswordInput.value !== '') {
            passwordMatchError.classList.remove('d-none');
            confirmPasswordInput.setCustomValidity("Passwords do not match"); // For HTML5 validation
            return false;
        } else {
            passwordMatchError.classList.add('d-none');
            confirmPasswordInput.setCustomValidity(""); // Reset validity
            return true;
        }
    };

    passwordInput.addEventListener('input', checkPasswordMatch);
    confirmPasswordInput.addEventListener('input', checkPasswordMatch);


    form.addEventListener('submit', async (event) => {
        event.preventDefault(); // Prevent default form submission
        hideMessage(); // Hide previous messages

        // Optional: Add Bootstrap's validation styles
        form.classList.add('was-validated');

        // Check password match again before submitting
        if (!checkPasswordMatch()) {
            showMessage('Passwords do not match.', 'warning');
            confirmPasswordInput.focus();
            return;
        }


        // Check basic HTML5 form validity
        if (!form.checkValidity()) {
            // Bootstrap or browser handles showing field errors due to was-validated class
            showMessage('Please fill out all required fields correctly.', 'warning');
            return;
        }


        const formData = new FormData(form);
        const data = Object.fromEntries(formData.entries());

        // Make sure age is sent as a number if backend expects it
        if (data.age) {
            data.age = parseInt(data.age, 10);
        }

        try {
            const response = await fetch('/api/auth/patient/register', { // Send to backend API endpoint
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(data),
            });

            const result = await response.json(); // Always parse JSON response

            if (response.ok && response.status === 201) {
                showMessage('Registration successful! Redirecting to login...', 'success');
                form.reset();
                form.classList.remove('was-validated');
                // Redirect to login page after a short delay
                setTimeout(() => {
                    window.location.href = '/patient/login'; // Use Thymeleaf's context path if needed: [[@{/patient/login}]] - though tricky in pure JS
                }, 2000);

            } else {
                // Handle errors from the backend (validation, duplicate email, etc.)
                let errorMessage = 'Registration failed. Please try again.';
                if (result.errors) { // Validation errors (from GlobalExceptionHandler)
                    const errorMessages = Object.values(result.errors).join(' ');
                    errorMessage = `Registration failed: ${errorMessages}`;
                } else if (result.message) { // Other errors (like DuplicateResourceException)
                    errorMessage = result.message;
                }
                showMessage(errorMessage, 'danger');
            }
        } catch (error) {
            console.error('Registration Error:', error);
            showMessage('An unexpected error occurred. Please check console or try again later.', 'danger');
        }
    });
});