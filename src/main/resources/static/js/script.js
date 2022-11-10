function resize(el) {
    $(el).toggleClass('flex-col');
}

function toggleInfo(el) {
    const parent = $(el).parent();
    const info = $(parent).find('.info');
    $(el).toggleClass('w-full');
    $(info).toggleClass('hidden');
    $(parent).toggleClass('flex-col');
    $(parent).toggleClass('w-[45%]');
    $(parent).toggleClass('w-full');
}