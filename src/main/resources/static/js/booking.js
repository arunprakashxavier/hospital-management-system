document.addEventListener('DOMContentLoaded', () => {
    // Get references to DOM elements
    const specializationSelect = document.getElementById('specialization');
    const doctorSelect = document.getElementById('doctor');
    const appointmentDateInput = document.getElementById('appointmentDate');
    const slotsContainer = document.getElementById('slotsContainer');
    const slotsPlaceholder = document.getElementById('slotsPlaceholder');
    const bookBtn = document.getElementById('bookBtn');
    const messageDiv = document.getElementById('bookingMessage');
    const selectedSlotTimeInput = document.getElementById('selectedSlotTime');
    const slotSelectionError = document.getElementById('slotSelectionError');
    const reasonInput = document.getElementById('reason'); // Get reference to reason input

    // --- Helper Functions ---
    const showMessage = (message, type = 'danger') => {
        messageDiv.textContent = message;
        messageDiv.className = `alert alert-${type} mt-3`; // Add margin top
        messageDiv.classList.remove('d-none');
    };

    const hideMessage = () => {
        messageDiv.classList.add('d-none');
        messageDiv.className = 'alert d-none';
        slotSelectionError.classList.add('d-none'); // Also hide slot error
        slotsContainer.classList.remove('is-invalid'); // Remove invalid state from slots container
    };

    const resetDoctors = () => {
        doctorSelect.innerHTML = '<option value="" selected disabled>-- Select Specialization First --</option>';
        doctorSelect.disabled = true;
    };

    const resetSlots = () => {
        slotsContainer.innerHTML = ''; // Clear previous slots
        slotsContainer.appendChild(slotsPlaceholder); // Add placeholder back
        slotsPlaceholder.textContent = 'Please select a doctor and date first.';
        slotsPlaceholder.classList.remove('d-none');
        selectedSlotTimeInput.value = ''; // Clear hidden input
        slotSelectionError.classList.add('d-none'); // Hide slot error
        slotsContainer.classList.remove('is-invalid');
    };

    const resetDate = () => {
        appointmentDateInput.value = '';
        appointmentDateInput.disabled = true;
    };

    const enableBookingButton = () => {
        // Enable only if specialization, doctor, date, AND slot are selected
        if (specializationSelect.value &&
            doctorSelect.value &&
            appointmentDateInput.value &&
            selectedSlotTimeInput.value) {
            bookBtn.disabled = false;
        } else {
            bookBtn.disabled = true;
        }
    };

    // Set minimum date for appointmentDate input (today)
    const today = new Date();
    // Adjust for timezone if needed, otherwise local timezone is used
    const yyyy = today.getFullYear();
    const mm = String(today.getMonth() + 1).padStart(2, '0'); // Months are 0-indexed
    const dd = String(today.getDate()).padStart(2, '0');
    appointmentDateInput.setAttribute('min', `${yyyy}-${mm}-${dd}`);


    // --- Event Listener for Specialization Change ---
    specializationSelect.addEventListener('change', async () => {
        hideMessage();
        resetDoctors();
        resetDate();
        resetSlots();
        enableBookingButton(); // Ensure button is disabled

        const selectedSpecialization = specializationSelect.value;

        if (!selectedSpecialization) {
            return; // Do nothing if default option is selected
        }

        doctorSelect.innerHTML = '<option value="" selected disabled>Loading doctors...</option>';
        doctorSelect.disabled = true;

        try {
            // Fetch doctors for the selected specialization
            const response = await fetch(`/api/doctors/specialization/${encodeURIComponent(selectedSpecialization)}`);

            if (!response.ok) {
                let errorMsg = `Error fetching doctors: ${response.statusText}`;
                try {
                    const errorData = await response.json();
                    errorMsg = errorData.message || errorMsg;
                } catch (e) { /* Ignore */ }
                throw new Error(errorMsg);
            }

            const doctors = await response.json();
            populateDoctors(doctors);

        } catch (error) {
            console.error('Error fetching doctors:', error);
            showMessage(`Could not load doctors. ${error.message}`, 'danger');
            resetDoctors();
        }
    });

    const populateDoctors = (doctors) => {
        if (!doctors || doctors.length === 0) {
            doctorSelect.innerHTML = '<option value="" selected disabled>-- No doctors found --</option>';
            doctorSelect.disabled = true;
            return;
        }

        doctorSelect.innerHTML = '<option value="" selected disabled>-- Select Doctor --</option>';
        doctors.forEach(doctor => {
            const option = document.createElement('option');
            option.value = doctor.id;
            option.textContent = `${doctor.name} (${doctor.qualification || doctor.specialization})`;
            doctorSelect.appendChild(option);
        });
        doctorSelect.disabled = false;
    };

    // --- Event Listener for Doctor Change ---
    doctorSelect.addEventListener('change', () => {
        hideMessage();
        resetSlots();
        resetDate(); // Reset date too, force re-selection
        enableBookingButton();

        if (doctorSelect.value) {
            appointmentDateInput.disabled = false;
        } else {
            appointmentDateInput.disabled = true; // Keep disabled if no doctor selected
        }
    });

    // --- Event Listener for Date Change ---
    appointmentDateInput.addEventListener('change', () => {
        hideMessage();
        resetSlots();
        enableBookingButton();

        const selectedDoctorId = doctorSelect.value;
        const selectedDate = appointmentDateInput.value;

        if (selectedDoctorId && selectedDate) {
            fetchAndDisplaySlots(selectedDoctorId, selectedDate);
        }
    });


    // --- Fetching and Displaying Slots ---
    const fetchAndDisplaySlots = async (doctorId, date) => {
        slotsPlaceholder.textContent = 'Loading slots...';
        slotsPlaceholder.classList.remove('d-none');
        slotsContainer.innerHTML = ''; // Clear previous slots/placeholder
        slotsContainer.appendChild(slotsPlaceholder);
        bookBtn.disabled = true; // Disable button until slot selected
        selectedSlotTimeInput.value = ''; // Clear selection
        slotSelectionError.classList.add('d-none');
        slotsContainer.classList.remove('is-invalid');

        try {
            // --- API Call ---
            const response = await fetch(`/api/doctors/${doctorId}/available-slots?date=${date}`);

            if (!response.ok) {
                let errorMsg = `Error fetching slots: ${response.statusText}`;
                try { const errorData = await response.json(); errorMsg = errorData.message || errorMsg; } catch (e) { /* Ignore */ }
                throw new Error(errorMsg);
            }

            const slots = await response.json(); // Expecting List<AvailableSlotDTO>

            // --- Render Slots ---
            slotsContainer.innerHTML = ''; // Clear "Loading..."
            if (!slots || slots.length === 0) {
                slotsContainer.innerHTML = '<p class="text-muted">No available slots found for this doctor on the selected date.</p>';
                selectedSlotTimeInput.value = '';
                enableBookingButton();
                return;
            }

            slots.forEach(slot => {
                const slotButton = document.createElement('button');
                slotButton.type = 'button';
                slotButton.classList.add('btn', 'btn-outline-primary', 'slot-button', 'm-1');

                const startTime = new Date(slot.startTime);
                const formattedTime = startTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: true });
                slotButton.textContent = formattedTime;
                slotButton.dataset.slotValue = slot.startTime; // Store ISO string

                slotButton.addEventListener('click', (event) => handleSlotSelection(event)); // Add listener

                slotsContainer.appendChild(slotButton);
            });

        } catch (error) {
            console.error('Error fetching slots:', error);
            showMessage(`Could not load slots. ${error.message}`, 'danger');
            slotsContainer.innerHTML = '<p class="text-danger">Could not load slots.</p>';
            selectedSlotTimeInput.value = '';
            enableBookingButton();
        }
    };

    // --- Handler for Slot Button Click ---
    const handleSlotSelection = (event) => {
        // Deselect all other buttons visually
        document.querySelectorAll('.slot-button').forEach(btn => {
            btn.classList.remove('selected-slot', 'btn-primary');
            btn.classList.add('btn-outline-primary');
        });

        // Select the clicked button visually
        const clickedButton = event.target;
        clickedButton.classList.add('selected-slot', 'btn-primary');
        clickedButton.classList.remove('btn-outline-primary');

        // Update the hidden input value with the full ISO date-time string
        selectedSlotTimeInput.value = clickedButton.dataset.slotValue;
        console.log("Selected Slot Time:", selectedSlotTimeInput.value); // Debugging

        // Hide any previous slot selection error
        slotSelectionError.classList.add('d-none');
        slotsContainer.classList.remove('is-invalid');

        // Check if booking button can be enabled
        enableBookingButton();
    };


    // --- Booking Submission ---
    bookBtn.addEventListener('click', async () => {
        hideMessage();

        // Validate: Check if a slot is selected
        if (!selectedSlotTimeInput.value) {
            slotSelectionError.textContent = 'Please select an available time slot.';
            slotSelectionError.classList.remove('d-none');
            slotsContainer.classList.add('is-invalid');
            return;
        } else {
            slotSelectionError.classList.add('d-none');
            slotsContainer.classList.remove('is-invalid');
        }

        // Disable button during submission
        bookBtn.disabled = true;
        bookBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Booking...';

        // Prepare data for API
        const bookingData = {
            doctorId: doctorSelect.value,
            requestedDateTime: selectedSlotTimeInput.value, // Value from hidden input
            reason: reasonInput.value.trim() || null // Use null if empty/whitespace
        };

        console.log("Booking Data to send:", bookingData);

        try {
            // --- API Call to book ---
            // Assumes session cookie provides authentication for this POST request
            // If using JWT, you would need to retrieve the token and add 'Authorization': 'Bearer ' + token header
            const response = await fetch('/api/appointments/book', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    // Add JWT header here if needed: 'Authorization': 'Bearer ' + yourStoredJwtToken
                },
                body: JSON.stringify(bookingData)
            });

            if (response.ok && response.status === 201) { // 201 Created
                const createdAppointment = await response.json(); // Get created appointment details
                showMessage(`Appointment successfully booked for ${new Date(createdAppointment.appointmentDateTime).toLocaleString()}! Appointment ID: ${createdAppointment.id}`, 'success');
                // Optionally reset form or redirect after a delay
                resetDoctors();
                resetDate();
                resetSlots();
                reasonInput.value = ''; // Clear reason input
                specializationSelect.value = ''; // Reset specialization dropdown
                enableBookingButton(); // Disable button again
                // setTimeout(() => { window.location.href = '/patient/appointments'; }, 3000); // Example redirect
            } else {
                // Handle backend errors (validation, conflicts, etc.)
                let errorMsg = `Booking failed: ${response.statusText}`;
                try {
                    const errorData = await response.json();
                    // Check for specific validation error structure from GlobalExceptionHandler
                    if (errorData.errors) {
                        errorMsg = "Booking failed: " + Object.values(errorData.errors).join('; ');
                    } else if (errorData.message) {
                        errorMsg = errorData.message; // Use message from ErrorDetails
                    }
                } catch (e) { /* Ignore if response is not JSON */ }
                throw new Error(errorMsg);
            }

        } catch (error) {
            console.error('Booking Error:', error);
            showMessage(`Appointment booking failed. ${error.message}`, 'danger');
        } finally {
            // Re-enable button regardless of success/error
            bookBtn.disabled = false; // Re-enable (or keep disabled based on enableBookingButton logic if form reset)
            enableBookingButton(); // Call this to set correct final disabled state
            bookBtn.innerHTML = 'Book Appointment'; // Reset button text
        }
    });

}); // End DOMContentLoaded