// sends post request to the url

var make_delete_fn= function(parent_selector) {
  return function(e) {
    // TODO: try doing this with clojurescript and domina
    $.ajax({
      url: $(this).attr('href'),
      type: "POST",
    });
    $(this).parentsUntil(parent_selector).remove();
    e.preventDefault();
  };
}

var delete_bookmark = make_delete_fn('.bookmark-list');
var delete_category = make_delete_fn('.manage-list');

$('a.delete-bookmark').click(delete_bookmark);
$('a.delete-category').click(delete_category);
