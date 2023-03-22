function resize(el) {
    $(el).toggleClass('flex-col');
}

function toggleInfo(el) {
    const parent = $(el).parent();
    const info = $(parent).find('.info');
    const img = $(parent).find('img');
    $(parent).toggleClass('absolute');
    $(el).toggleClass('w-1/2');
    $(info).toggleClass('hidden');
    $(parent).toggleClass('flex-col');
    $(parent).toggleClass('w-[45%]');
    $(parent).toggleClass('max-h-[45%]');
    $(parent).toggleClass('w-11/12');
    $(parent).toggleClass('h-5/6');
    $(img).toggleClass('h-full');
}