// Simple UI JS to call /dynamic/fetch and /dynamic/download
(function () {
  const fieldsByEntity = {
    Employee: ['id','name','salary','joiningDate','department.name','active'],
    Department: ['id','name']
  };

  let state = {
    entity: 'Employee',
    fields: ['id','name','salary'],
    filters: {},
    page: 0,
    size: 10,
    sort: '',     // legacy single sort
    sorts: [],    // multi-sort
    distinct: false,
    lastExportPath: null
  };

  const entitySelect = document.getElementById('entitySelect');
  const fieldsList = document.getElementById('fieldsList');
  const pageSize = document.getElementById('pageSize');
  const pageNumber = document.getElementById('pageNumber');
  const sortInput = document.getElementById('sortInput');

  const filtersContainer = document.getElementById('filtersContainer');
  const addEquals = document.getElementById('addEquals');
  const addLike = document.getElementById('addLike');
  const addBetween = document.getElementById('addBetween');
  const clearFilters = document.getElementById('clearFilters');

  const sortsContainer = document.getElementById('sortsContainer');
  const addSortBtn = document.getElementById('addSortBtn');

  const fetchBtn = document.getElementById('fetchBtn');
  const exportBtn = document.getElementById('exportBtn');
  const downloadBtn = document.getElementById('downloadBtn');
  const tableArea = document.getElementById('tableArea');
  const pageInfo = document.getElementById('pageInfo');
  const prevPage = document.getElementById('prevPage');
  const nextPage = document.getElementById('nextPage');
  const sampleSelect = document.getElementById('sampleSelect');
  const applySample = document.getElementById('applySample');
  const distinctChk = document.getElementById('distinctChk');

  // ---------------------- Fields rendering ----------------------
  function renderFields() {
    fieldsList.innerHTML = '';
    const fields = fieldsByEntity[state.entity] || [];
    fields.forEach(f => {
      const id = 'chk_' + f.replace(/\W/g, '_');
      const div = document.createElement('div');
      div.innerHTML = `<label><input type="checkbox" ${state.fields.includes(f)?'checked':''} data-field="${f}" id="${id}"> ${f}</label>`;
      fieldsList.appendChild(div);
      div.querySelector('input').addEventListener('change', function(){
        const fld = this.getAttribute('data-field');
        if (this.checked) {
          if (!state.fields.includes(fld)) state.fields.push(fld);
        } else {
          state.fields = state.fields.filter(x=>x!==fld);
        }
      });
    });
  }

  // ---------------------- Filters rendering ----------------------
  function renderFilters() {
    filtersContainer.innerHTML = '';
    const keys = Object.keys(state.filters || {});
    if (keys.length === 0) {
      filtersContainer.innerHTML = '<em>No filters</em>';
      return;
    }
    keys.forEach(k => {
      const v = state.filters[k];
      const row = document.createElement('div');
      row.className = 'filter-row';
      row.innerHTML = `<strong>${k}</strong> : <span>${JSON.stringify(v)}</span> <button class="btn small secondary" data-key="${k}">Remove</button>`;
      filtersContainer.appendChild(row);
      row.querySelector('button').addEventListener('click', function(){
        delete state.filters[this.getAttribute('data-key')];
        renderFilters();
      });
    });
  }

  function addFilter(type) {
    const field = prompt('Field name (e.g. name, salary, joiningDate, department.name):');
    if (!field) return;
    if (type === 'equals') {
      const val = prompt('Value for equals:');
      if (val == null) return;
      state.filters[field] = val;
    } else if (type === 'like') {
      const val = prompt('Value for LIKE (partial):');
      if (val == null) return;
      state.filters[field + '_like'] = val;
    } else if (type === 'between') {
      const a = prompt('Start value:');
      const b = prompt('End value:');
      if (a == null || b == null) return;
      const na = isFinite(Number(a)) ? Number(a) : a;
      const nb = isFinite(Number(b)) ? Number(b) : b;
      state.filters[field + '_between'] = [na, nb];
    }
    renderFilters();
  }

  // ---------------------- Multi-sort UI ----------------------
  function renderSorts() {
    sortsContainer.innerHTML = '';
    if (!state.sorts || state.sorts.length === 0) {
      sortsContainer.innerHTML = '<em>No multi-sort defined</em>';
      return;
    }

    const fields = fieldsByEntity[state.entity] || [];

    state.sorts.forEach((spec, idx) => {
      const row = document.createElement('div');
      row.className = 'sort-row';

      // field select
      const fieldSelect = document.createElement('select');
      if (fields.length === 0) {
        const opt = document.createElement('option');
        opt.value = spec.field || '';
        opt.textContent = spec.field || '(field)';
        fieldSelect.appendChild(opt);
      } else {
        fields.forEach(f => {
          const opt = document.createElement('option');
          opt.value = f;
          opt.textContent = f;
          if (spec.field === f) opt.selected = true;
          fieldSelect.appendChild(opt);
        });
      }

      fieldSelect.addEventListener('change', () => {
        state.sorts[idx].field = fieldSelect.value;
      });

      // direction select
      const dirSelect = document.createElement('select');
      ['asc','desc'].forEach(d => {
        const opt = document.createElement('option');
        opt.value = d;
        opt.textContent = d.toUpperCase();
        if ((spec.direction || 'asc').toLowerCase() === d) opt.selected = true;
        dirSelect.appendChild(opt);
      });
      dirSelect.addEventListener('change', () => {
        state.sorts[idx].direction = dirSelect.value;
      });

      // remove button
      const removeBtn = document.createElement('button');
      removeBtn.type = 'button';
      removeBtn.className = 'btn small secondary';
      removeBtn.textContent = 'X';
      removeBtn.addEventListener('click', () => {
        state.sorts.splice(idx, 1);
        renderSorts();
      });

      row.appendChild(fieldSelect);
      row.appendChild(dirSelect);
      row.appendChild(removeBtn);
      sortsContainer.appendChild(row);
    });
  }

  function addSortRow() {
    const fields = fieldsByEntity[state.entity] || [];
    const defaultField = fields.length > 0 ? fields[0] : 'id';
    state.sorts = state.sorts || [];
    state.sorts.push({
      field: defaultField,
      direction: 'asc'
    });
    renderSorts();
  }

  // ---------------------- Build payload & fetch ----------------------
  function buildPayload(exportMode=false) {
    return {
      entity: state.entity,
      fields: state.fields,
      filters: state.filters,
      page: state.page,
      size: state.size,
      sort: state.sort,     // legacy
      sorts: state.sorts,   // multi
      distinct: state.distinct,
      export: exportMode
    };
  }

  async function doFetch(exportMode=false) {
    const payload = buildPayload(exportMode);
    tableArea.innerHTML = '<em>Loading...</em>';
    try {
      const res = await fetch('/dynamic/fetch', {
        method: 'POST',
        headers: {'Content-Type':'application/json'},
        body: JSON.stringify(payload)
      });
      if (!res.ok) throw new Error('Server error: ' + res.status);
      const data = await res.json();

      if (exportMode) {
        if (data.file) {
          state.lastExportPath = data.file;
          downloadBtn.style.display = 'inline-block';
          alert('Export ready: ' + data.file + '\nClick Download to fetch the file.');
        } else {
          alert('Export response: ' + JSON.stringify(data));
        }
        tableArea.innerHTML = '<em>Export finished</em>';
        return;
      }

      renderTable(data);
    } catch (err) {
      console.error(err);
      tableArea.innerHTML = '<em>Error fetching data</em>';
      alert('Error: ' + err.message);
    }
  }

  function renderTable(resp) {
    const rows = resp.content || [];
    if (!rows || rows.length === 0) {
      tableArea.innerHTML = '<em>No results</em>';
    } else {
      const fields = state.fields;
      const table = document.createElement('table');
      const thead = document.createElement('thead');
      const tr = document.createElement('tr');
      fields.forEach(f => {
        const th = document.createElement('th');
        th.textContent = f;
        tr.appendChild(th);
      });
      thead.appendChild(tr);
      table.appendChild(thead);
      const tbody = document.createElement('tbody');
      rows.forEach(r => {
        const tr = document.createElement('tr');
        fields.forEach(f => {
          const td = document.createElement('td');
          let v = r[f];
          if (v === null || v === undefined) v = '';
          td.textContent = v;
          tr.appendChild(td);
        });
        tbody.appendChild(tr);
      });
      table.appendChild(tbody);
      tableArea.innerHTML = '';
      tableArea.appendChild(table);
    }
    pageInfo.textContent = `Page ${resp.page+1} of ${resp.totalPages} (Total: ${resp.totalElements})`;
  }

  // ---------------------- Events ----------------------
  entitySelect.addEventListener('change', function(){
    state.entity = this.value;
    state.fields = fieldsByEntity[state.entity].slice(0,3);
    // when entity changes, reset sorts to avoid invalid fields
    state.sorts = [];
    renderFields();
    renderFilters();
    renderSorts();
  });

  pageSize.addEventListener('change', function(){ state.size = Number(this.value) || 10; });
  pageNumber.addEventListener('change', function(){ state.page = Number(this.value) || 0; });
  sortInput.addEventListener('change', function(){ state.sort = this.value; });
  distinctChk.addEventListener('change', function(){ state.distinct = this.checked; });

  addEquals.addEventListener('click', ()=>addFilter('equals'));
  addLike.addEventListener('click', ()=>addFilter('like'));
  addBetween.addEventListener('click', ()=>addFilter('between'));
  clearFilters.addEventListener('click', ()=>{ state.filters = {}; renderFilters(); });

  addSortBtn.addEventListener('click', addSortRow);

  fetchBtn.addEventListener('click', ()=>{
    state.page = Number(pageNumber.value)||0;
    state.size = Number(pageSize.value)||10;
    state.sort = sortInput.value;
    doFetch(false);
  });

  exportBtn.addEventListener('click', ()=>{
    state.page = 0;
    state.size = Number(pageSize.value)||1000;
    state.sort = sortInput.value;
    doFetch(true);
  });

  downloadBtn.addEventListener('click', ()=>{
    if (!state.lastExportPath) {
      alert('No export');
      return;
    }
    const url = '/dynamic/download?filePath=' + encodeURIComponent(state.lastExportPath);
    window.open(url, '_blank');
  });

  prevPage.addEventListener('click', ()=>{
    if (state.page>0) {
      state.page--;
      pageNumber.value = state.page;
      doFetch(false);
    }
  });

  nextPage.addEventListener('click', ()=>{
    state.page++;
    pageNumber.value = state.page;
    doFetch(false);
  });

  applySample.addEventListener('click', ()=>{
    const val = sampleSelect.value;
    if (val === '1') {
      state.entity='Employee';
      state.fields=['id','name','salary'];
      state.filters={'name_like':'John'};
      state.page=0; state.size=10; state.distinct=false;
      state.sorts = [];
    } else if (val === '2') {
      state.entity='Employee';
      state.fields=['id','name','salary'];
      state.filters={'salary_between':[30000,80000]};
      state.page=0; state.size=10; state.distinct=false;
      state.sorts = [{ field: 'salary', direction: 'desc' }];
    } else if (val === '3') {
      state.entity='Employee';
      state.fields=['department.name'];
      state.filters={};
      state.page=0; state.size=20; state.distinct=true;
      state.sorts = [{ field: 'department.name', direction: 'asc' }];
    } else if (val === '4') {
      state.entity='Employee';
      state.fields=['id','name','department.name'];
      state.filters={
        'department.id_inSubquery': {
          entity: 'Department',
          field: 'id',
          filters: { 'name_like': 'Sales' }
        }
      };
      state.page=0; state.size=10; state.distinct=false;
      state.sorts = [{ field: 'name', direction: 'asc' }];
    } else {
      return;
    }

    entitySelect.value = state.entity;
    pageNumber.value = state.page;
    pageSize.value = state.size;
    sortInput.value = state.sort;
    distinctChk.checked = state.distinct;

    renderFields();
    renderFilters();
    renderSorts();
    doFetch(false);
  });

  // ---------------------- Init ----------------------
  renderFields();
  renderFilters();
  renderSorts();
})();
