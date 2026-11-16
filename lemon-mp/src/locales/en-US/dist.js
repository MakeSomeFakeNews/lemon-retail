/** Store requisitions and receiving (dist.xxx) */
export default {
  entry: {
    requisition: 'Requisitions',
    receive: 'Store Receiving'
  },
  requisition: {
    add: 'New Requisition',
    edit: 'Edit Requisition',
    warehouse: 'Receiving Warehouse',
    bizDate: 'Requisition Date',
    expectedDate: 'Expected Arrival',
    qty: 'Requested Qty',
    stock: 'On hand {n}',
    transit: 'In transit {n}',
    suggest: 'Suggested {n}',
    supply: 'Supply available {n}',
    approved: 'Approved {n}',
    delivered: 'Delivered {n}',
    addMaterial: 'Add Item',
    scanTip: 'Scan a barcode or enter a code',
    useSuggest: 'Use suggestions',
    saveDraft: 'Save Draft',
    saveSubmit: 'Save & Submit',
    submitConfirm: 'It cannot be modified after submission. Submit?',
    noItems: 'Please add items',
    noQty: 'Please enter the requested quantity',
    items: '{n} item(s)',
    rejectReason: 'Rejected: {reason}',
    notFound: 'Item {code} not found'
  },
  receive: {
    tip: 'Deliveries waiting to be received by your store',
    from: 'From: {name}',
    shipped: 'Shipped {n}',
    received: 'Received',
    damaged: 'Damaged',
    reason: 'Note',
    receiveDate: 'Receiving Date',
    confirm: 'Confirm Receipt',
    confirmDiff: '{n} line(s) differ from the shipped quantity and will create receiving discrepancies. Receive?',
    confirmAll: 'Receive everything as shipped?',
    serialTip: 'Serial-tracked items must be received in full',
    done: 'Received'
  }
}
