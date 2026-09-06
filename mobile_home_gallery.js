var carouselIndex = 0;

function drawGallery() {
    var dotDiv = document.getElementById("gallery_dot_progress");
    var limit = document.getElementById("full_available_list").childElementCount
    for (let i = 0; i < limit; i++) {
        var newDot = document.createElement("div");
        newDot.className = "gallery_dot";
        dotDiv.appendChild(newDot);
    }

    var width = document.querySelector(".nav_box_mobile").width;
    var remainingWidth = width - (limit * 20);
    dotDiv.style = "margin-left: " + (remainingWidth / 2) + "px; grid-column: 1 / 3;";
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

    var toDisplay = document.getElementById("full_available_list").children[carouselIndex];
    var nextImage = toDisplay.getAttribute("src");
    var link = toDisplay.getAttribute("href");
    var text = toDisplay.getAttribute("horse_title");

    document.getElementById("mobile_home_gallery_text").textContent = text;
    document.getElementById("mobile_home_gallery_display").src = nextImage;
    document.getElementById("avail_gallery_outer").href = link;

    var dotProgress = document.getElementById("gallery_dot_progress");
    for (let i = 0; i < dotProgress.childElementCount; i++) {
        dotProgress.children[i].style = "background-color: lightgrey;";
    }
    dotProgress.children[carouselIndex].style = "background-color: darkgrey;";
}

function makeSafe(bound) {
    var numChildren = document.getElementById("full_available_list").childElementCount;
    if (bound < 0) {
        return bound + numChildren;
    }
    if (bound >= numChildren) {
        return bound - numChildren;
    }
    return bound;
}