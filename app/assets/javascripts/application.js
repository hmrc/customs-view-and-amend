// Functionality for customs-view-and-amend service

const CustomsViewAndAmend = {

    Init: function () {
        // Add back link
        const backLinkAnchor = document.querySelector('.govuk-back-link');

        if (backLinkAnchor) {
            backLinkAnchor.addEventListener('click', function (event) {
                event.preventDefault();
                history.back();
            });
        }

        // Open feedback link in new window
        const feedbackLink = document.querySelector('.govuk-phase-banner a.govuk-link');

        if (feedbackLink) {
            feedbackLink.setAttribute('target', '_blank');
        }

    }
}

window.addEventListener('load', CustomsViewAndAmend.Init);






// =====================================================
// Back link mimics browser back functionality
// =====================================================
// store referrer value to cater for IE - https://developer.microsoft.com/en-us/microsoft-edge/platform/issues/10474810/  */
const docReferrer = document.referrer

// prevent resubmit warning
if (window.history && window.history.replaceState && typeof window.history.replaceState === 'function') {
    window.history.replaceState(null, null, window.location.href);
}