(function () {
  function ready(callback) {
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', callback);
      return;
    }
    callback();
  }

  function options(select) {
    return Array.prototype.slice.call(select.options || []);
  }

  function parentValues(parent) {
    return options(parent)
      .filter(function (option) {
        return option.selected && option.value;
      })
      .map(function (option) {
        return option.value;
      });
  }

  function resetSelection(select) {
    if (select.multiple) {
      options(select).forEach(function (option) {
        option.selected = false;
      });
      return;
    }
    const fallback = options(select).find(function (option) {
      return !option.disabled && option.value === '';
    }) || options(select).find(function (option) {
      return !option.disabled;
    });
    select.value = fallback ? fallback.value : '';
  }

  function notifySelectChanged(select) {
    select.dispatchEvent(new Event('change', { bubbles: true }));
    if (window.jQuery && window.jQuery.fn && window.jQuery.fn.select2) {
      window.jQuery(select).trigger('change.select2');
    }
  }

  function syncDependentSelect(select) {
    const parentSelector = select.dataset.parentSelect;
    const parent = parentSelector ? document.querySelector(parentSelector) : null;
    if (!parent) {
      return;
    }

    const selectedParents = parentValues(parent);
    let invalidSelection = false;

    options(select).forEach(function (option) {
      const parentValue = option.dataset.parentValue;
      if (!parentValue) {
        option.hidden = false;
        option.disabled = false;
        return;
      }

      const visible = selectedParents.length > 0 && selectedParents.indexOf(parentValue) >= 0;
      option.hidden = !visible;
      option.disabled = !visible;
      if (!visible && option.selected) {
        invalidSelection = true;
      }
    });

    if (invalidSelection) {
      resetSelection(select);
      notifySelectChanged(select);
      return;
    }

    if (window.jQuery && window.jQuery.fn && window.jQuery.fn.select2) {
      window.jQuery(select).trigger('change.select2');
    }
  }

  ready(function () {
    const dependentSelects = Array.prototype.slice.call(document.querySelectorAll('[data-dependent-select]'));
    dependentSelects.forEach(function (select) {
      const parent = document.querySelector(select.dataset.parentSelect);
      if (!parent) {
        return;
      }
      parent.addEventListener('change', function () {
        syncDependentSelect(select);
      });
      syncDependentSelect(select);
    });
  });
})();
