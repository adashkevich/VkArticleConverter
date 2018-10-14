// progressive-image.js, v1.2
// by Craig Buckler, @craigbuckler
if (window.addEventListener && window.requestAnimationFrame && document.getElementsByClassName) window.addEventListener('load', function () {

    // start
    var pictureItem = document.getElementsByClassName('progressive replace'), pCount, timer;

    // scroll and resize events
    window.addEventListener('scroll', scroller, false);
    window.addEventListener('resize', scroller, false);

    // DOM mutation observer
    if (MutationObserver) {

        var observer = new MutationObserver(function () {
            if (pictureItem.length !== pCount) inView();
        });
        observer.observe(document.body, {subtree: true, childList: true, attributes: true, characterData: true});

    }

    // initial check
    inView();


    // throttled scroll/resize
    function scroller() {

        timer = timer || setTimeout(function () {
            timer = null;
            inView();
        }, 300);

    }


    // image in view?
    function inView() {

        if (pictureItem.length) requestAnimationFrame(function () {

            var windowTop = window.pageYOffset, windowBottom = windowTop + window.innerHeight,
                screen = window.innerHeight, cRect, pictureTop, pictureBottom, p = 0;
            while (p < pictureItem.length) {

                cRect = pictureItem[p].getBoundingClientRect();
                pictureTop = windowTop + cRect.top;
                pictureBottom = pictureTop + cRect.height;

                if (windowTop - screen < pictureBottom && windowBottom + screen > pictureTop) {
                    loadFullImage(pictureItem[p]);
                    pictureItem[p].classList.remove('replace');
                }
                else p++;
            }
            pCount = pictureItem.length;
        });
    }


    // replace with full image
    function loadFullImage(item) {

        var href = item && (item.getAttribute('data-href') || item.href);
        if (!href) return;
        // load image
        var img = new Image();
        if (item.dataset) {
            img.srcset = item.dataset.srcset || '';
            img.sizes = item.dataset.sizes || '';
        }
        img.src = href;
        img.className = 'reveal';
        if (img.complete) {
            addImg();
        } else {
            img.onload = addImg;
        }
        // replace image
        function addImg() {
            requestAnimationFrame(function () {
                // disable click
                if (href === item.href) {
                    item.style.cursor = 'default';
                    item.addEventListener('click', function (e) {
                        e.preventDefault();
                    }, false);
                }
                // preview image
                var pImg = item.querySelector && item.querySelector('img.preview');
                // add full image
                item.insertBefore(img, pImg && pImg.nextSibling).addEventListener('animationend', function () {
                    // remove preview image
                    if (pImg) {
                        img.alt = pImg.alt || '';
                        item.removeChild(pImg);
                    }
                    img.classList.remove('reveal');

                });

            });

        }
    }
}, false);
