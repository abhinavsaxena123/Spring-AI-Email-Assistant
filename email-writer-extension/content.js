console.log("Email Writer Extension - Content Script Loaded");

// Create the AI Reply button element
function createAIButton() {
    const button = document.createElement('div');  // Create a div element
    button.className = 'T-I J-J5-Ji aoO v7 T-I-atl L3'; // Using Gmail internal CSS class
    button.style.marginRight = '8px';
    button.innerHTML = 'AI Reply';
    button.setAttribute('role', 'button'); // Make this div act like a button
    button.setAttribute('data-tooltip', 'Generate AI Reply'); // When users hover over button, it shows "Generate AI Reply" 
    return button;
}

// Extracts the original email content to provide context for AI reply generation
function getEmailContent() {
    // Gmail's DOM structure changes frequently, so multiple selectors are needed.
    const selectors = [
        '.h7',
        '.a3s.aiL',
        'gmail_quote',
        '[role = presentation]'
    ];

    for(const selector of selectors) {
        const content = document.querySelector(selector);

        if(content) {
            return content.innerText.trim();
        }
    }

    return '';
}

// Finds Gmail's compose window toolbar where the button should be inserted
function findComposeToolbar() {
    const selectors = [
        '.btC',
        '.aDh',
        '[role = toolbar]',
        '.gU.Up'
    ];

    for(const selector of selectors) {
        const toolbar = document.querySelector(selector);

        if(toolbar) {
            return toolbar;
        }
    }

    return null;
}

// Main function that adds the AI button to Gmail's interface
function injectButton() {
    console.log("Injecting button...");

    // Remove existing button to prevent duplicates
    const existingButton = document.querySelector('.ai-reply-button');
    if(existingButton) {
        existingButton.remove();
    }

    // Find the toolbar
    const toolbar = findComposeToolbar();
    if(!toolbar) {
        console.log("Toolbar not found");
        return;
    }

    console.log("Toolbar found, creating AI button");

    // Create and setup the button
    const button = createAIButton();
    button.classList.add('ai-reply-button');

    // Add click event listener
    button.addEventListener('click', async () => {
        try {
            // Show loading state
            button.innerHTML = 'Generating...';
            button.disabled = true;

            // Get email content for context
            const emailContent = getEmailContent();

            // Call AI API
            const response = await fetch('http://localhost:8080/api/email/generate', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body : JSON.stringify({
                    emailContent: emailContent,
                    tone: "professional"
                })
            });

            if(!response.ok) {
                throw new Error('API Request Failed!')
            }

            // Get generated reply
            const generatedReply = await response.text();

            // Insert into compose box
            const composeBox = document.querySelector('[role="textbox"][g_editable="true"]');

            if(composeBox) {
                composeBox.focus();

                // Select all existing content
                document.execCommand('selectAll', false, null);

                // Replace with new content
                document.execCommand('insertText', false, generatedReply);

                // document.execCommand('insertText', false, generatedReply);
            } else {
                console.error('Compose Box not found');
            }

        } catch (error) {
            console.error(error);
            alert('Failed to generate reply');
        } finally {
            // Reset button state
            button.innerHTML = 'AI Reply';
            button.disabled = false;
        }
    });

    // Insert button at the beginning of toolbar
    toolbar.insertBefore(button, toolbar.firstChild);

}

// Watches for new compose windows and automatically adds the AI button
const observer = new MutationObserver( (mutations) => {
    for(const mutation of mutations) {

        const addedNodes = Array.from(mutation.addedNodes);

        const hasComposedElements = addedNodes.some(node => 
            node.nodeType === Node.ELEMENT_NODE && 
            (node.matches('.aDh, .btC [role="dialog"]') || node.querySelector('.aDh, .btC [role="dialog"]'))
        );

        if(hasComposedElements) {
            console.log("Compose Window Detected");
            setTimeout(injectButton, 500);
        }
    }
});

observer.observe(document.body, {
    childList: true,
    subtree: true 
})