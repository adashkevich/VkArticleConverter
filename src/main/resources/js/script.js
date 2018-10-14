//add class for mobile device
if( /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent) ) {
	document.getElementsByClassName('article')[0].classList.add('article_mobile');
	document.getElementsByClassName('article_layer__content')[0].classList.remove('article_layer__content');
}