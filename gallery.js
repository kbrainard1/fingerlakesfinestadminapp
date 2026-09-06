var carouselIndex = 0;

function drawGallery() {
    var dotDiv = document.getElementById("gallery_dot_progress");
    var limit = document.getElementById("image_gallery_full").childElementCount
    for (let i = 0; i < limit; i++) {
        var newDot = document.createElement("div");
        newDot.className = "gallery_dot";
        dotDiv.appendChild(newDot);
    }
    displayImage();
}

function displayFullSize(index) {
    carouselIndex = index;
    displayImage();
}

function prevImage() {
    carouselIndex--;
    carouselIndex = makeSafe(carouselIndex);
    displayImage();
}

function nextImage() {
    carouselIndex++;
    carouselIndex = makeSafe(carouselIndex);
    displayImage();
}

function displayImage() {
    var fullGallery = document.getElementById("image_gallery_full");
    var nextImage = fullGallery.children[carouselIndex].getAttribute("full_size");
    document.getElementById("full_size_image").src = nextImage;
    var dotProgress = document.getElementById("gallery_dot_progress");
    for (let i = 0; i < dotProgress.childElementCount; i++) {
        dotProgress.children[i].style = "background-color: lightgrey;";
    }
    dotProgress.children[carouselIndex].style = "background-color: darkgrey;";
}

function makeSafe(bound) {
    var numChildren = document.getElementById("image_gallery_full").childElementCount;
    if (bound < 0) {
        return bound + numChildren;
    }
    if (bound >= numChildren) {
        return bound - numChildren;
    }
    return bound;
}

// "Scale this image to at most 80% of the page in terms of width/height,
// and then set the outer div to those dimensions" is not easily expressed
// in CSS in both firefox and chromium.
function resizeImage() {
    var windowWidth = window.innerWidth;
    var windowHeight = window.innerHeight;

    // ugh, mobile
    var screenWidth = screen.availWidth;
    var screenHeight = screen.availHeight;
    if (windowWidth > screenWidth) {
        windowWidth = screenWidth;
    }
    if (windowHeight > screenHeight) {
        windowHeight = screenHeight;
    }

    var smallWindowWidth = windowWidth * 0.8;
    var smallWindowHeight = windowHeight * 0.8;

    // determine limiting dimension for image scaling
    var image = document.getElementById("full_size_image")
    const imageWidth = image.naturalWidth;
    const imageHeight = image.naturalHeight;

    const widthFraction = smallWindowWidth / imageWidth;
    const heightFraction = smallWindowHeight / imageHeight;
    var adjustedWidth = imageWidth;
    var adjustedHeight = imageHeight;
    if (widthFraction < 1 || heightFraction < 1) {
        if (widthFraction < heightFraction) {
            adjustedWidth = imageWidth * widthFraction;
            adjustedHeight = imageHeight * widthFraction;
        } else {
            adjustedWidth = imageWidth * heightFraction;
            adjustedHeight = imageHeight * heightFraction;
        }
    }

    // update visible component dimensions
    image.style.width = adjustedWidth + "px";
    image.style.height = adjustedHeight + "px";

    var prevImage = document.querySelector(".prev_image");
    var nextImage = document.querySelector(".next_image");
    prevImage.style.fontSize = (adjustedHeight * .1) + "px";
    prevImage.style.paddingTop = (adjustedHeight * .45) + "px";
    nextImage.style.fontSize = (adjustedHeight * .1) + "px";
    nextImage.style.paddingTop = (adjustedHeight * .45) + "px";
}