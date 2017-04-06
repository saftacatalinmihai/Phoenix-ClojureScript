defmodule GenServerProxy do
  use GenServer

  def start_link(actor_type) do
    GenServer.start_link(__MODULE__, actor_type, [])
  end

  def init(actor_type) do
    :"Elixir.#{actor_type}".start_link
  end
  
  def handle_call(msg, from, pid) do
    old_state = :sys.get_state(pid)
    resp = GenServer.call(pid, msg)
    new_state = :sys.get_state(pid)
    GenServer.cast(EventStore,
      %{:from => from, 
        :to => pid, 
        :msg => msg, 
        :resp => resp, 
        :state_tranzition => {old_state, new_state}})
    {:reply, resp, pid}
  end

  def code_change(old_vsn, old_state, _extra) do
    {:ok, old_state}
  end

end
