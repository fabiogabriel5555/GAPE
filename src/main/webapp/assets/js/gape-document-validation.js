(function () {
    'use strict';

    var TYPE_MESSAGES = {
        CITIZEN_CARD: 'Citizen Card number must use the DDDDDDDD C AAT format and pass the check digit validation.',
        TAX_IDENTIFICATION_NUMBER: 'NIF must contain 9 digits and pass the control digit validation.',
        PASSPORT: 'Passport number can only contain 6 to 9 letters or digits.',
        RESIDENCE_PERMIT: 'Residence permit number can contain only letters, digits, spaces or hyphens, and must have 6 to 12 characters after separators are removed.'
    };

    function compact(value) {
        return value.toUpperCase().replace(/[\s-]/g, '');
    }

    function normalize(type, value) {
        if (type === 'CITIZEN_CARD' || type === 'TAX_IDENTIFICATION_NUMBER' || type === 'RESIDENCE_PERMIT') {
            return compact(value);
        }
        return value.trim().toUpperCase();
    }

    function citizenCardValue(character) {
        if (character >= '0' && character <= '9') {
            return character.charCodeAt(0) - 48;
        }
        if (character >= 'A' && character <= 'Z') {
            return character.charCodeAt(0) - 55;
        }
        return -1;
    }

    function validCitizenCard(value) {
        var sum = 0;
        var secondDigit = false;

        for (var index = value.length - 1; index >= 0; index--) {
            var digitValue = citizenCardValue(value.charAt(index));
            if (digitValue < 0) {
                return false;
            }
            if (secondDigit) {
                digitValue *= 2;
                if (digitValue > 9) {
                    digitValue -= 9;
                }
            }
            sum += digitValue;
            secondDigit = !secondDigit;
        }

        return sum % 10 === 0;
    }

    function validTaxIdentificationNumber(value) {
        var sum = 0;
        for (var index = 0; index < 8; index++) {
            sum += Number(value.charAt(index)) * (9 - index);
        }
        var remainder = sum % 11;
        var checkDigit = remainder < 2 ? 0 : 11 - remainder;
        return checkDigit === Number(value.charAt(8));
    }

    function validTaxIdentificationPrefix(value) {
        return '12356789'.indexOf(value.charAt(0)) >= 0 || value.indexOf('45') === 0;
    }

    function invalidCharacterMessage(type, value) {
        if (type === 'PASSPORT' && /[^A-Za-z0-9]/.test(value)) {
            return 'Passport number can only contain letters A-Z and digits 0-9.';
        }
        if (type === 'TAX_IDENTIFICATION_NUMBER' && /[^0-9\s-]/.test(value)) {
            return 'NIF can only contain digits.';
        }
        if ((type === 'CITIZEN_CARD' || type === 'RESIDENCE_PERMIT') && /[^A-Za-z0-9\s-]/.test(value)) {
            return TYPE_MESSAGES[type];
        }
        return '';
    }

    function validateDocument(type, value) {
        var rawValue = value.trim();
        if (!type && !rawValue) {
            return { valid: true, normalized: '', message: '' };
        }
        if (!type || !rawValue) {
            return {
                valid: false,
                normalized: '',
                message: 'Document type and document number must be filled together.'
            };
        }

        var invalidCharacters = invalidCharacterMessage(type, rawValue);
        if (invalidCharacters) {
            return { valid: false, normalized: rawValue, message: invalidCharacters };
        }

        var normalized = normalize(type, rawValue);
        var valid = true;
        if (type === 'CITIZEN_CARD') {
            valid = /^[0-9]{9}[A-Z0-9]{2}[0-9]$/.test(normalized) && validCitizenCard(normalized);
        } else if (type === 'TAX_IDENTIFICATION_NUMBER') {
            valid = /^[0-9]{9}$/.test(normalized)
                && validTaxIdentificationPrefix(normalized)
                && validTaxIdentificationNumber(normalized);
        } else if (type === 'PASSPORT') {
            valid = /^[A-Z0-9]{6,9}$/.test(normalized);
        } else if (type === 'RESIDENCE_PERMIT') {
            valid = /^[A-Z0-9]{6,12}$/.test(normalized);
        }

        if (!valid) {
            return {
                valid: false,
                normalized: normalized,
                message: TYPE_MESSAGES[type] || 'Document number is invalid for the selected document type.'
            };
        }
        return { valid: true, normalized: normalized, message: '' };
    }

    function visibleSelectSelection(select) {
        var sibling = select.nextElementSibling;
        if (sibling && sibling.classList.contains('select2-container')) {
            return sibling.querySelector('.select2-selection');
        }
        return null;
    }

    function setFieldError(control, hasError) {
        control.classList.toggle('gape-document-field-error', hasError);
        var visibleSelection = visibleSelectSelection(control);
        if (visibleSelection) {
            visibleSelection.classList.toggle('gape-document-field-error', hasError);
        }
    }

    function ensureMessage(numberInput) {
        var messageId = numberInput.id ? numberInput.id + 'ValidationMessage' : 'documentNumberValidationMessage';
        var message = document.getElementById(messageId);
        if (!message) {
            message = document.createElement('p');
            message.id = messageId;
            message.className = 'gape-document-validation-message text-14 text-danger-600 mt-8 mb-0 d-none';
            numberInput.insertAdjacentElement('afterend', message);
        }

        var describedBy = numberInput.getAttribute('aria-describedby') || '';
        if (describedBy.split(/\s+/).indexOf(messageId) === -1) {
            numberInput.setAttribute('aria-describedby', (describedBy + ' ' + messageId).trim());
        }
        return message;
    }

    function setMessage(messageElement, text) {
        messageElement.textContent = text || '';
        messageElement.classList.toggle('d-none', !text);
    }

    function initForm(form) {
        var documentType = form.querySelector('[name="documentType"]');
        var documentNumber = form.querySelector('[name="documentNumber"]');
        if (!documentType || !documentNumber || documentNumber.dataset.gapeDocumentValidation === 'true') {
            return;
        }

        documentNumber.dataset.gapeDocumentValidation = 'true';
        var messageElement = ensureMessage(documentNumber);

        function syncDocumentValidation() {
            var typeValue = documentType.value.trim();
            var numberValue = documentNumber.value.trim();
            var documentStarted = typeValue !== '' || numberValue !== '';
            var result = validateDocument(typeValue, numberValue);

            documentType.required = documentStarted;
            documentNumber.required = documentStarted;
            documentNumber.setCustomValidity(result.message);
            setFieldError(documentNumber, !result.valid);
            setFieldError(documentType, !result.valid && documentStarted);
            setMessage(messageElement, result.message);
            return result;
        }

        documentType.addEventListener('input', syncDocumentValidation);
        documentType.addEventListener('change', syncDocumentValidation);
        documentNumber.addEventListener('input', syncDocumentValidation);
        documentNumber.addEventListener('blur', function () {
            var result = syncDocumentValidation();
            if (result.valid && result.normalized) {
                documentNumber.value = result.normalized;
            }
        });
        form.addEventListener('reset', function () {
            window.setTimeout(syncDocumentValidation, 0);
        });
        form.addEventListener('submit', function (event) {
            var result = syncDocumentValidation();
            if (!result.valid) {
                event.preventDefault();
                event.stopPropagation();
                documentNumber.reportValidity();
                return;
            }
            if (result.normalized) {
                documentNumber.value = result.normalized;
            }
        });

        syncDocumentValidation();
    }

    function init(root) {
        var scope = root || document;
        var forms = scope.querySelectorAll('form');
        forms.forEach(initForm);
    }

    window.GapeDocumentValidation = {
        init: init,
        validateDocument: validateDocument
    };

    document.addEventListener('DOMContentLoaded', function () {
        init(document);
    });
}());
